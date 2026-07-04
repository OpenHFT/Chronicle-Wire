/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.IgnoresEverything;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A stable, per-source {@link MetricsOut} facade following the pattern of
 * {@code net.openhft.chronicle.core.onoes.ThreadLocalisedExceptionHandler}: it owns a shared
 * <em>default</em> {@code MetricsOut} plus a {@link ThreadLocal} <em>override</em>, resolving
 * per call as <i>thread-local if set, else default</i> and skipping delegation entirely when
 * the resolved handler {@code instanceof} {@link IgnoresEverything}.
 * <p>
 * Global management is one-place mutation: installing a binding, toggling a source at runtime
 * or restoring defaults after a test swaps the default via {@link #defaultHandler(MetricsOut)}
 * and every thread observes it on its next call. Test harnesses install per-thread captures
 * via {@link #threadLocal(MetricsOut)} and remove them with {@link #resetThreadLocal()}.
 * <p>
 * Delegate failures are contained: the first throwing sink is warned, the event is dropped,
 * and later calls continue best-effort without surfacing sink failures to product code.
 * <p>
 * This facade is deliberately <em>not</em> {@link IgnoresEverything} itself: callers wanting
 * to skip expensive sample preparation should check {@code unwrap() instanceof IgnoresEverything}.
 */
public class ThreadLocalisedMetricsOut implements MetricsOut {

    private volatile MetricsOut defaultOut;
    private final ThreadLocal<MetricsOut> outTL = new ThreadLocal<>();
    private final AtomicBoolean failureLogged = new AtomicBoolean();

    /**
     * Creates a facade using the supplied handler as the shared default.
     *
     * @param defaultOut the handler used when no thread-local override is present;
     *                   {@code null} means the shared ignored handler
     */
    public ThreadLocalisedMetricsOut(MetricsOut defaultOut) {
        defaultOut = unwrap(defaultOut);
        this.defaultOut = defaultOut == null ? Metrics.ignored() : defaultOut;
    }

    @Override
    public void counterMetric(CounterMetric metric) {
        MetricsOut out = resolve();
        if (out instanceof IgnoresEverything)
            return;
        try {
            out.counterMetric(metric);
        } catch (Throwable t) {
            sinkFailure(t);
        }
    }

    @Override
    public void gaugeMetric(GaugeMetric metric) {
        MetricsOut out = resolve();
        if (out instanceof IgnoresEverything)
            return;
        try {
            out.gaugeMetric(metric);
        } catch (Throwable t) {
            sinkFailure(t);
        }
    }

    @Override
    public void histogramMetric(HistogramMetric metric) {
        MetricsOut out = resolve();
        if (out instanceof IgnoresEverything)
            return;
        try {
            out.histogramMetric(metric);
        } catch (Throwable t) {
            sinkFailure(t);
        }
    }

    @Override
    public void rateMetric(RateMetric metric) {
        MetricsOut out = resolve();
        if (out instanceof IgnoresEverything)
            return;
        try {
            out.rateMetric(metric);
        } catch (Throwable t) {
            sinkFailure(t);
        }
    }

    @Override
    public void pointEvent(PointEvent metric) {
        MetricsOut out = resolve();
        if (out instanceof IgnoresEverything)
            return;
        try {
            out.pointEvent(metric);
        } catch (Throwable t) {
            sinkFailure(t);
        }
    }

    private MetricsOut resolve() {
        MetricsOut out = outTL.get();
        if (out == null)
            out = defaultOut;
        return out;
    }

    private void sinkFailure(Throwable t) {
        if (failureLogged.compareAndSet(false, true))
            Jvm.warn().on(ThreadLocalisedMetricsOut.class,
                    "Metrics sink threw; dropping this and further sink failures for this source facade", t);
    }

    /**
     * Returns the handler this facade currently resolves to for the calling thread:
     * the thread-local override if set, else the shared default. Callers use this to skip
     * expensive sample preparation: {@code if (out.unwrap() instanceof IgnoresEverything) ...}
     *
     * @return the resolved handler
     */
    public MetricsOut unwrap() {
        return resolve();
    }

    /**
     * Unwraps the supplied handler if it is an instance of this class.
     *
     * @param out the handler to unwrap
     * @return the underlying resolved handler, or the supplied instance if not wrapped
     */
    public static MetricsOut unwrap(MetricsOut out) {
        if (out instanceof ThreadLocalisedMetricsOut)
            return ((ThreadLocalisedMetricsOut) out).resolve();
        return out;
    }

    /**
     * Returns the shared default handler.
     *
     * @return the default handler in use
     */
    public MetricsOut defaultHandler() {
        return defaultOut;
    }

    /**
     * Swaps the shared default handler; every thread observes the new default on its next
     * call (unless it has a thread-local override installed).
     *
     * @param defaultOut the new default, or {@code null} for the shared ignored handler
     * @return this instance for chaining
     */
    public ThreadLocalisedMetricsOut defaultHandler(MetricsOut defaultOut) {
        defaultOut = unwrap(defaultOut);
        this.defaultOut = defaultOut == null ? Metrics.ignored() : defaultOut;
        failureLogged.set(false);
        return this;
    }

    /**
     * Returns the override for the current thread, or {@code null} if none is set.
     *
     * @return the thread-local override, or {@code null}
     */
    public MetricsOut threadLocal() {
        return outTL.get();
    }

    /**
     * Installs an override for the current thread only; other threads keep resolving to the
     * shared default (or their own overrides).
     *
     * @param out the override for this thread; {@code null} removes the override, like
     *            {@link #resetThreadLocal()}
     * @return this instance for chaining
     */
    public ThreadLocalisedMetricsOut threadLocal(MetricsOut out) {
        if (out == null)
            outTL.remove();
        else
            outTL.set(out);
        failureLogged.set(false);
        return this;
    }

    /**
     * Removes the current thread's override so it resolves to the shared default again.
     */
    public void resetThreadLocal() {
        outTL.remove();
        failureLogged.set(false);
    }
}
