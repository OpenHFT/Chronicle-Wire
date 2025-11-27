/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.MethodWriterInvocationHandler;
import net.openhft.chronicle.core.io.Closeable;

import java.util.function.Supplier;

/**
 * Configurable factory for {@link MethodWriterInvocationHandler} instances. It
 * wraps a delegate supplier and applies common options such as history
 * recording, close behaviour, generic event name, method ID usage and
 * thread-safety mode.  Instances are lazily created and cached per thread unless
 * thread safety is explicitly disabled.
 * <p>
 * This supplier is primarily used by
 * {@link VanillaMethodWriterBuilder} when generating method writer proxies.
 */
public class MethodWriterInvocationHandlerSupplier implements Supplier<MethodWriterInvocationHandler> {

    // The main supplier delegate that provides the base MethodWriterInvocationHandler instances.
    private final Supplier<MethodWriterInvocationHandler> supplier;

    // whether created handlers should record invocation history
    private boolean recordHistory;
    // optional resource to close when the handler is closed
    private Closeable closeable;
    // if true a single non-thread-safe instance will be reused
    private boolean disableThreadSafe;
    // event name used when the method writer is generic
    private String genericEvent;
    // if false the handler avoids writing method identifiers
    private boolean useMethodIds = true;

    // thread-local cache for handlers when thread safety is enabled
    private final ThreadLocal<MethodWriterInvocationHandler> handlerTL =
            ThreadLocal.withInitial(this::newHandler);

    // shared instance used when thread safety is disabled
    private MethodWriterInvocationHandler handler;

    /**
     * Creates a supplier that wraps the provided delegate.
     *
     * @param supplier delegate supplier, commonly returning either a
     *                 {@link BinaryMethodWriterInvocationHandler} or a
     *                 {@link TextMethodWriterInvocationHandler}
     */
    public MethodWriterInvocationHandlerSupplier(Supplier<MethodWriterInvocationHandler> supplier) {
        this.supplier = supplier;
    }

    /**
     * Enable or disable invocation history recording on new handlers.
     */
    public void recordHistory(boolean recordHistory) {
        this.recordHistory = recordHistory;
    }

    /**
     * Assign a resource to be closed when the handler closes.
     */
    public void onClose(Closeable closeable) {
        this.closeable = closeable;
    }

    /**
     * Sets the configuration for thread safety.
     */
    public void disableThreadSafe(boolean disableThreadSafe) {
        this.disableThreadSafe = disableThreadSafe;
    }

    /**
     * Set the event name used by generic method writers.
     */
    public void genericEvent(String genericEvent) {
        this.genericEvent = genericEvent;
    }

    /**
     * Determines whether method identifiers are emitted.
     */
    @Deprecated(/* to be removed in 2027 */)
    public void useMethodIds(boolean useMethodIds) {
        this.useMethodIds = useMethodIds;
    }

    /**
     * Internal helper to create a new handler and apply all configured options.
     */
    private MethodWriterInvocationHandler newHandler() {
        MethodWriterInvocationHandler h = supplier.get();
        h.genericEvent(genericEvent);
        h.onClose(closeable);
        h.recordHistory(recordHistory);
        h.useMethodIds(useMethodIds);
        return h;
    }

    /**
     * Returns a configured handler.  A thread-local instance is supplied by
     * default; when {@link #disableThreadSafe} is true a shared instance is
     * returned.  Each instance is created lazily via {@link #newHandler()}.
     */
    @Override
    public MethodWriterInvocationHandler get() {
        if (disableThreadSafe) {
            if (handler == null) {
                handler = newHandler();
            }
            return handler;
        }
        return handlerTL.get();
    }
}
