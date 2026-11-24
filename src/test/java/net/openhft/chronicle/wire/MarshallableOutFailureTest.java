/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import static org.junit.Assert.*;

import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.function.Consumer;

public class MarshallableOutFailureTest extends WireTestCommon {

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(
                Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
    }

    private static class Output implements MarshallableOut {
        final DocumentContext context;
        int closes, rollbacks, texts;

        Output(RuntimeException writeFailure, RuntimeException closeFailure) {
            ValueOut value =
                    proxy(
                            ValueOut.class,
                            (p, m, a) -> {
                                if (m.getName().equals("text")) texts++;
                                return null;
                            });
            Wire wire =
                    proxy(
                            Wire.class,
                            (p, m, a) -> {
                                if (writeFailure != null) throw writeFailure;
                                return value;
                            });
            context =
                    proxy(
                            DocumentContext.class,
                            (p, m, a) -> {
                                switch (m.getName()) {
                                    case "wire":
                                        return wire;
                                    case "rollbackOnClose":
                                        rollbacks++;
                                        return null;
                                    case "close":
                                        closes++;
                                        if (closeFailure != null) throw closeFailure;
                                        return null;
                                    default:
                                        throw new AssertionError(m.getName());
                                }
                            });
        }

        public DocumentContext writingDocument(boolean metadata) {
            return context;
        }

        public DocumentContext acquireWritingDocument(boolean metadata) {
            return context;
        }
    }

    private void preservesFailure(Consumer<MarshallableOut> call) {
        RuntimeException original = new IllegalArgumentException("write failed");
        RuntimeException cleanup = new IllegalStateException("close failed");
        Output output = new Output(original, cleanup);
        RuntimeException failure = assertThrows(RuntimeException.class, () -> call.accept(output));
        assertSame(original, failure);
        assertArrayEquals(new Throwable[] {cleanup}, failure.getSuppressed());
        assertEquals(1, output.rollbacks);
        assertEquals(1, output.closes);
    }

    @Test
    public void keyMessagePreservesWriteFailure() {
        preservesFailure(out -> out.writeMessage((WireKey) () -> "event", 1));
    }

    @Test
    public void namedMessagePreservesWriteFailure() {
        preservesFailure(out -> out.writeMessage("event", 1));
    }

    @Test
    public void bytesMessagePreservesWriteFailure() {
        preservesFailure(out -> out.writeBytes(bytes -> {}));
    }

    @Test
    public void valueDocumentPreservesWriteFailure() {
        preservesFailure(out -> out.writeDocument(1, (value, item) -> {}));
    }

    @Test
    public void textMessagePreservesWriteFailure() {
        preservesFailure(out -> out.writeText("text"));
    }

    @Test
    public void mapMessagePreservesWriteFailure() {
        preservesFailure(out -> out.writeMap(Collections.singletonMap("key", 1)));
    }

    @Test
    public void successfulWriteStillReportsCloseFailure() {
        IllegalStateException cleanup = new IllegalStateException("close failed");
        Output output = new Output(null, cleanup);
        assertSame(
                cleanup, assertThrows(IllegalStateException.class, () -> output.writeText("text")));
        assertEquals(1, output.texts);
        assertEquals(0, output.rollbacks);
    }

    @Test
    public void successfulWriteClosesExactlyOnce() {
        Output output = new Output(null, null);
        output.writeText("text");
        assertEquals(1, output.texts);
        assertEquals(1, output.closes);
        assertEquals(0, output.rollbacks);
    }
}
