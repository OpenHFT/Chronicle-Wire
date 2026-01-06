/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.MethodWriterInvocationHandler;
import net.openhft.chronicle.core.io.Closeable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class MethodWriterInvocationHandlerSupplierTest extends WireTestCommon {

    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("supplier applies configuration to new handlers")
    void supplierAppliesConfigurationToNewHandlers() {
        Supplier<MethodWriterInvocationHandler> delegate = RecordingHandler::new;
        MethodWriterInvocationHandlerSupplier supplier = new MethodWriterInvocationHandlerSupplier(delegate);
        RecordingCloseable closeable = new RecordingCloseable();

        supplier.recordHistory(true);
        supplier.onClose(closeable);
        supplier.genericEvent("event");
        supplier.useMethodIds(false);

        RecordingHandler handler = (RecordingHandler) supplier.get();

        assertEquals(true, handler.recordHistory, "supplier should set the recordHistory flag");
        assertSame(closeable, handler.closeable, "supplier should set the closeable target");
        assertEquals("event", handler.genericEvent, "supplier should set the generic event string");
        assertEquals(false, handler.useMethodIds, "supplier should set the method id mode flag");
    }

    @Test
    @DisplayName("thread-local handlers are distinct across threads")
    void threadLocalHandlersAreDistinctAcrossThreads() throws InterruptedException {
        MethodWriterInvocationHandlerSupplier supplier = new MethodWriterInvocationHandlerSupplier(RecordingHandler::new);

        RecordingHandler mainHandler = (RecordingHandler) supplier.get();
        AtomicReference<RecordingHandler> otherHandler = new AtomicReference<>();

        Thread thread = new Thread(() -> otherHandler.set((RecordingHandler) supplier.get()));
        thread.start();
        thread.join();

        assertNotSame(mainHandler, otherHandler.get(), "Supplier uses thread-local handlers by default");
    }

    @Test
    @DisplayName("disableThreadSafe reuses one shared handler instance")
    void disableThreadSafeReusesSharedHandler() {
        MethodWriterInvocationHandlerSupplier supplier = new MethodWriterInvocationHandlerSupplier(RecordingHandler::new);
        supplier.disableThreadSafe(true);

        MethodWriterInvocationHandler first = supplier.get();
        MethodWriterInvocationHandler second = supplier.get();

        assertSame(first, second, "supplier should reuse the handler when thread safety is disabled");
    }

    @Test
    @DisplayName("thread-local supplier reuses handler in the same thread")
    void threadLocalSupplierReusesHandlerInSameThread() {
        MethodWriterInvocationHandlerSupplier supplier = new MethodWriterInvocationHandlerSupplier(RecordingHandler::new);

        MethodWriterInvocationHandler first = supplier.get();
        MethodWriterInvocationHandler second = supplier.get();

        assertSame(first, second, "Supplier reuses the thread-local handler for the same thread");
    }

    private static final class RecordingHandler implements MethodWriterInvocationHandler {
        private boolean recordHistory;
        private Closeable closeable;
        private String genericEvent;
        private boolean useMethodIds = true;

        @Override
        public void recordHistory(boolean recordHistory) {
            this.recordHistory = recordHistory;
        }

        @Override
        public void onClose(Closeable closeable) {
            this.closeable = closeable;
        }

        @Override
        public void genericEvent(String genericEvent) {
            this.genericEvent = genericEvent;
        }

        @Override
        public void useMethodIds(boolean useMethodIds) {
            this.useMethodIds = useMethodIds;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            return null;
        }
    }

    private static final class RecordingCloseable implements Closeable {
        private boolean closed;

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }
    }
}
