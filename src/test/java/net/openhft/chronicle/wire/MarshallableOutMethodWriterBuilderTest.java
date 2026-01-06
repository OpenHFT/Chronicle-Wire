/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.MethodWriterBuilder;
import net.openhft.chronicle.core.io.Closeable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class MarshallableOutMethodWriterBuilderTest extends WireTestCommon {

    @Test
    @DisplayName("Method writer builder wires the Closeable output")
    void methodWriterBuilderSetsCloseable() throws Exception {
        CloseableMarshallableOut out = new CloseableMarshallableOut();
        MethodWriterBuilder<Runnable> builder = out.methodWriterBuilder(Runnable.class);
        Closeable closeable = extractCloseable(builder);
        assertSame(out, closeable, "Closeable output should be registered on the builder");
    }

    @Test
    @DisplayName("Method writer builder ignores non-Closeable outputs")
    void methodWriterBuilderSkipsCloseable() throws Exception {
        NonCloseableMarshallableOut out = new NonCloseableMarshallableOut();
        MethodWriterBuilder<Runnable> builder = out.methodWriterBuilder(Runnable.class);
        Closeable closeable = extractCloseable(builder);
        assertNull(closeable, "Non-Closeable output should not be registered");
    }

    private static Closeable extractCloseable(MethodWriterBuilder<?> builder) throws Exception {
        VanillaMethodWriterBuilder<?> concrete = (VanillaMethodWriterBuilder<?>) builder;
        Field field = VanillaMethodWriterBuilder.class.getDeclaredField("closeable");
        field.setAccessible(true);
        return (Closeable) field.get(concrete);
    }

    private static class NonCloseableMarshallableOut implements MarshallableOut {
        @Override
        public DocumentContext writingDocument() {
            throw new UnsupportedOperationException("Not used in this test");
        }

        @Override
        public DocumentContext writingDocument(boolean metaData) {
            throw new UnsupportedOperationException("Not used in this test");
        }

        @Override
        public DocumentContext acquireWritingDocument(boolean metaData) {
            throw new UnsupportedOperationException("Not used in this test");
        }
    }

    private static final class CloseableMarshallableOut extends NonCloseableMarshallableOut implements Closeable {
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
