/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.IgnoresEverything;
import net.openhft.chronicle.core.util.Mocker;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The user-facing acquisition point for metrics, keyed by dotted <em>source</em> name and
 * selected like log levels: a library grabs its sink with {@code Metrics.forSource(...)} (or
 * registers instruments with {@link #registry(String)} / {@link #newRegistry(String)}) from
 * anywhere, without plumbing, and whoever owns the process decides where the events go by
 * installing a {@link MetricsBinding} once at startup.
 * <p>
 * {@link #forSource(String)} always returns the source's stable
 * {@link ThreadLocalisedMetricsOut} facade (the {@code Jvm} exception-handler pattern),
 * created on first acquisition. Its default is the shared {@link Mocker#ignored} proxy until
 * a binding is installed; {@link #install(MetricsBinding)} re-resolves every facade's default,
 * so handles cached in {@code static final} fields observe later installs, runtime toggles and
 * uninstalls - a one-place default swap every thread sees on its next call. Callers that
 * accept resolve-once semantics in exchange for the maximally cheap,
 * {@code instanceof}-skippable raw proxy use {@link #forSourceStatic(String)}.
 * <p>
 * {@code Metrics} never throws into callers: acquisition never returns {@code null}, and a
 * {@link MetricsBinding#outFor(String)} that throws is logged once via {@link Jvm#warn()} and
 * treated as leaving that source ignored.
 * <p>
 * <b>Test isolation.</b> Prefer scoped installs -
 * {@code try (Metrics.Installation i = Metrics.install(binding)) { ... }} restores the
 * previous binding on close - and call {@link #resetForTesting()} in an {@code @After}/tear-down
 * to restore the pristine state (no binding, every facade ignored, process-global registries
 * closed and cleared) so sequential test cases do not see each other's instruments.
 */
public final class Metrics {

    // The shared never-null, never-throws default; implements IgnoresEverything.
    private static final MetricsOut IGNORED = Mocker.ignored(MetricsOut.class);

    // Process-global source registry; touched only at acquisition.
    private static final Map<String, ThreadLocalisedMetricsOut> SOURCES = new ConcurrentHashMap<>();

    // One process-global registry per source; independent registries come from newRegistry().
    private static final Map<String, MetricsRegistry> REGISTRIES = new ConcurrentHashMap<>();

    private static volatile MetricsBinding binding;

    // Guarded by the Metrics.class lock; reset when the binding changes.
    private static boolean warnedBindingThrew;

    private Metrics() {
    }

    /**
     * Returns the shared ignored {@link MetricsOut}; implements {@link IgnoresEverything}.
     *
     * @return the shared ignored handler
     */
    public static MetricsOut ignored() {
        return IGNORED;
    }

    /**
     * Returns the stable {@link ThreadLocalisedMetricsOut} facade for the given source,
     * created on first acquisition. Its default is the shared ignored proxy until a binding
     * is installed; installing (or uninstalling) a binding re-resolves it, so a handle cached
     * in a {@code static final} field observes later {@link #install(MetricsBinding)} calls.
     * To skip expensive sample preparation, check
     * {@code out.unwrap() instanceof IgnoresEverything} - the facade itself is deliberately
     * not {@link IgnoresEverything}. For a resolve-once raw handler, see
     * {@link #forSourceStatic(String)}.
     *
     * @param source the dotted source name, e.g. {@code chronicle.queue.replication}
     * @return the facade for the source; never {@code null}, never throws
     */
    public static MetricsOut forSource(String source) {
        return threadLocalised(source);
    }

    /**
     * Convenience overload: uses the class's fully qualified name as the dotted source.
     *
     * @param source the class whose name becomes the source
     * @return the facade for the source; never {@code null}, never throws
     */
    public static MetricsOut forSource(Class<?> source) {
        return forSource(source.getName());
    }

    /**
     * Resolve-once fast path: returns the handler the installed binding currently resolves
     * the source to - the shared ignored proxy ({@code instanceof IgnoresEverything},
     * skippable at the call site with no facade indirection) when no binding is installed or
     * the source is disabled. Unlike {@link #forSource(String)}, the returned handle does
     * <b>not</b> observe a later {@link #install(MetricsBinding)}: use it only where maximum
     * cheapness matters and dynamic switching is accepted to be off.
     *
     * @param source the dotted source name
     * @return the currently resolved handler; never {@code null}, never throws
     */
    public static MetricsOut forSourceStatic(String source) {
        return resolve(source);
    }

    /**
     * Returns the process-global instrument registry for the given source, creating it on
     * first use (or after the previous one was {@linkplain MetricsRegistry#close() closed}).
     * A convenience for simple cases; components that own their flush cadence - event loops,
     * services - should use {@link #newRegistry(String)} instead.
     *
     * @param source the dotted source name the instruments are registered under
     * @return the registry for the source
     */
    public static MetricsRegistry registry(String source) {
        return REGISTRIES.compute(source,
                (s, r) -> r == null || r.isClosed() ? new MetricsRegistry(s) : r);
    }

    /**
     * Creates a new, independent registry the caller owns and flushes: it is not shared with
     * {@link #registry(String)} or with any other {@code newRegistry} call, so its
     * {@link MetricsRegistry#flush(MetricsOut, long, long) flush} touches only the caller's
     * instruments. This is what event loops and services should use - one registry per owner,
     * flushed on that owner's cadence - and {@link MetricsRegistry#close()} when the owner
     * shuts down.
     *
     * @param source the dotted source name the instruments are registered under
     * @return a new registry owned by the caller
     */
    public static MetricsRegistry newRegistry(String source) {
        return new MetricsRegistry(source);
    }

    /**
     * Installs the process-level binding, re-resolving every known source facade's default
     * handler - one-place mutation which every thread observes on its next call. A binding
     * whose {@link MetricsBinding#outFor(String)} throws is logged once via {@link Jvm#warn()}
     * and the throwing source treated as ignored.
     *
     * @param binding the binding to install, or {@code null} to restore the ignored default
     * @return a handle whose {@link Installation#close() close()} restores the binding that
     *         was installed before this call and re-resolves the facades; idempotent, so it
     *         is safe in try-with-resources
     */
    public static synchronized Installation install(MetricsBinding binding) {
        MetricsBinding previous = Metrics.binding;
        Metrics.binding = binding;
        warnedBindingThrew = false;
        for (Map.Entry<String, ThreadLocalisedMetricsOut> entry : SOURCES.entrySet())
            entry.getValue().defaultHandler(resolve(entry.getKey()));
        return new Installation(previous);
    }

    /**
     * Restores the pristine state for test isolation: uninstalls any binding, swaps every
     * known facade's default back to the shared ignored proxy (cached
     * {@link #forSource(String)} handles stay valid and simply go quiet), and closes and
     * clears the process-global registries so the next {@link #registry(String)} call starts
     * empty. Independent {@link #newRegistry(String)} instances are unaffected - their owners
     * close them.
     * <p>
     * Intended pattern: call from an {@code @After}/tear-down method in any test that
     * installs a binding or registers instruments through {@link #registry(String)}.
     */
    public static synchronized void resetForTesting() {
        binding = null;
        warnedBindingThrew = false;
        for (ThreadLocalisedMetricsOut out : SOURCES.values())
            out.defaultHandler(IGNORED);
        for (MetricsRegistry registry : REGISTRIES.values())
            registry.close();
        REGISTRIES.clear();
    }

    /**
     * Returns the stable {@link ThreadLocalisedMetricsOut} facade for a source, creating it on
     * first use with a default resolved from the installed binding. Intended for frameworks
     * and test harnesses that manage per-thread overrides; {@link #forSource(String)} returns
     * the same instance typed as {@link MetricsOut}.
     *
     * @param source the dotted source name
     * @return the source's facade
     */
    public static ThreadLocalisedMetricsOut threadLocalised(String source) {
        ThreadLocalisedMetricsOut out = SOURCES.get(source);
        if (out == null)
            out = SOURCES.computeIfAbsent(source, Metrics::newSource);
        return out;
    }

    private static ThreadLocalisedMetricsOut newSource(String source) {
        return new ThreadLocalisedMetricsOut(resolve(source));
    }

    private static MetricsOut resolve(String source) {
        MetricsBinding b = binding;
        if (b == null)
            return IGNORED;
        MetricsOut out;
        try {
            out = b.outFor(source);
        } catch (Throwable t) {
            warnBindingThrew(source, t);
            return IGNORED;
        }
        return out == null ? IGNORED : out;
    }

    private static synchronized void warnBindingThrew(String source, Throwable t) {
        if (warnedBindingThrew)
            return;
        warnedBindingThrew = true;
        Jvm.warn().on(Metrics.class,
                "MetricsBinding.outFor(\"" + source + "\") threw; treating the source as ignored. " +
                        "Further failures of this binding will not be logged.", t);
    }

    /**
     * The handle returned by {@link Metrics#install(MetricsBinding)}: closing it restores the
     * binding that was installed when it was created and re-resolves every facade, enabling
     * scoped installs via try-with-resources. {@link #close()} is idempotent and never throws.
     */
    public static final class Installation implements AutoCloseable {

        private final MetricsBinding previous;
        private boolean closed;

        Installation(MetricsBinding previous) {
            this.previous = previous;
        }

        /**
         * Restores the previously installed binding (possibly {@code null}, the ignored
         * default) and re-resolves every facade. Idempotent; never throws.
         */
        @Override
        public void close() {
            synchronized (Metrics.class) {
                if (closed)
                    return;
                closed = true;
                install(previous);
            }
        }
    }
}
