/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.MethodWriterInvocationHandler;
import net.openhft.chronicle.core.io.Closeable;

import java.util.function.Supplier;

/**
 * The {@code MethodWriterInvocationHandlerSupplier} class is an implementation of the {@link Supplier} interface
 * for providing instances of {@link MethodWriterInvocationHandler}. This supplier offers a series of
 * configurations which influence the behavior of the generated {@link MethodWriterInvocationHandler} instances.
 * Configurable behaviors include thread safety, recording history, and more.
 */
public class MethodWriterInvocationHandlerSupplier implements Supplier<MethodWriterInvocationHandler> {

    // The main supplier delegate that provides the base MethodWriterInvocationHandler instances.
    private final Supplier<MethodWriterInvocationHandler> supplier;

    // Configuration fields
    private boolean recordHistory;
    private Closeable closeable;
    private boolean disableThreadSafe;
    private String genericEvent;
    private boolean useMethodIds = true;

    // A thread-local handler for thread-safe operations
    private final ThreadLocal<MethodWriterInvocationHandler> handlerTL = ThreadLocal.withInitial(this::newHandler);

    // A non-thread safe handler instance
    private MethodWriterInvocationHandler handler;

    /**
     * Constructs a new {@code MethodWriterInvocationHandlerSupplier} with a delegate supplier.
     *
     * @param supplier The delegate supplier that provides base instances of {@link MethodWriterInvocationHandler}.
     */
    public MethodWriterInvocationHandlerSupplier(Supplier<MethodWriterInvocationHandler> supplier) {
        this.supplier = supplier;
    }

    /**
     * Sets the configuration for recording history.
     *
     * @param recordHistory Whether to enable history recording.
     */
    public void recordHistory(boolean recordHistory) {
        this.recordHistory = recordHistory;
    }

    /**
     * Sets a {@link Closeable} instance to be invoked when closing.
     *
     * @param closeable The Closeable instance.
     */
    public void onClose(Closeable closeable) {
        this.closeable = closeable;
    }

    /**
     * Sets the configuration for thread safety.
     *
     * @param disableThreadSafe Whether to disable thread safety.
     */
    public void disableThreadSafe(boolean disableThreadSafe) {
        this.disableThreadSafe = disableThreadSafe;
    }

    /**
     * Sets the generic event to be associated with the handler.
     *
     * @param genericEvent The generic event as a string.
     */
    public void genericEvent(String genericEvent) {
        this.genericEvent = genericEvent;
    }

    /**
     * Configures whether to use method IDs.
     *
     * @param useMethodIds Whether to enable method IDs.
     */
    public void useMethodIds(boolean useMethodIds) {
        this.useMethodIds = useMethodIds;
    }

    /**
     * Creates and initializes a new {@link MethodWriterInvocationHandler} using the current configurations.
     *
     * @return A newly initialized MethodWriterInvocationHandler.
     */
    private MethodWriterInvocationHandler newHandler() {
        MethodWriterInvocationHandler h = supplier.get();
        h.genericEvent(genericEvent);
        h.onClose(closeable);
        h.recordHistory(recordHistory);
        h.useMethodIds(useMethodIds);
        return h;
    }

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
