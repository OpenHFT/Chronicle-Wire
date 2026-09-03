/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodWriterInvocationHandler;
import net.openhft.chronicle.core.io.Closeable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static net.openhft.chronicle.wire.VanillaMethodWriterBuilder.DISABLE_WRITER_PROXY_CODEGEN;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class WireContextListenerLifecycleTest extends WireTestCommon {
    private static final WireType[] WRITABLE_WIRE_TYPES = {
            WireType.BINARY,
            WireType.TEXT,
            WireType.YAML,
            WireType.RAW
    };

    private final List<Bytes<?>> allocatedBytes = new ArrayList<>();

    @Before
    public void allowExpectedListenerDiagnostics() {
        ignoreException("Context listener failed:");
    }

    @After
    public void releaseBytes() {
        allocatedBytes.forEach(Bytes::releaseLast);
        allocatedBytes.clear();
    }

    @Test
    public void contextListenerWaitsForDataAfterMetadataAndWritesDtoOnce() {
        Wire wire = newWire(WireType.BINARY);
        AtomicInteger calls = new AtomicInteger();
        WireOut configured = wire.contextListener(ContextEvents.class, writer -> {
            calls.incrementAndGet();
            writer.context(new ContextData("schema", 7));
        });
        assertSame(wire, configured);

        try (DocumentContext dc = wire.writingDocument(true)) {
            dc.wire().write("header").text("metadata");
        }

        assertEquals(0, calls.get());
        assertThrows(IllegalStateException.class,
                () -> wire.contextListener(ContextEvents.class, writer -> {
                }));

        ContextEvents writer = wire.methodWriter(ContextEvents.class);
        writer.event(new EventData("one", 1));
        writer.event(new EventData("two", 2));

        assertEquals(1, calls.get());
        assertEquals("" +
                        "--- !!meta-data #binary\n" +
                        "header: metadata\n" +
                        "# position: 20, header: 0\n" +
                        "--- !!data #binary\n" +
                        "context: {\n" +
                        "  name: schema,\n" +
                        "  version: 7\n" +
                        "}\n" +
                        "# position: 60, header: 1\n" +
                        "--- !!data #binary\n" +
                        "event: {\n" +
                        "  name: one,\n" +
                        "  sequence: 1\n" +
                        "}\n" +
                        "# position: 96, header: 2\n" +
                        "--- !!data #binary\n" +
                        "event: {\n" +
                        "  name: two,\n" +
                        "  sequence: 2\n" +
                        "}\n",
                Wires.fromSizePrefixedBlobs(wire));
    }

    @Test
    public void listenerFailurePoisonsCurrentContextUntilReset() {
        Wire wire = newWire(WireType.BINARY);
        AtomicInteger calls = new AtomicInteger();
        wire.contextListener(ContextEvents.class, writer -> {
            calls.incrementAndGet();
            writer.context(new ContextData("schema", 7));
            if (calls.get() == 1)
                throw new IllegalStateException("listener failed");
        });

        ContextEvents writer = wire.methodWriter(ContextEvents.class);
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> writer.event(new EventData("failed", 1)));
        assertEquals("listener failed", thrown.getMessage());

        IllegalStateException poisoned = assertThrows(IllegalStateException.class,
                () -> writer.event(new EventData("after", 2)));
        assertEquals("Context listener failed for the current output context", poisoned.getMessage());

        assertEquals(1, calls.get());
        try (DocumentContext context = wire.readingDocument()) {
            assertTrue(context.isPresent());
            assertEquals("context", context.wire().readEvent(String.class));
            assertEquals(new ContextData("schema", 7),
                    context.wire().getValueIn().object(ContextData.class));
        }
        try (DocumentContext context = wire.readingDocument()) {
            assertFalse(context.isPresent());
        }

        wire.reset();
        writer.event(new EventData("after-reset", 3));

        assertEquals(2, calls.get());
    }

    @Test
    public void applicationSerializationFailurePoisonsSuccessfulContextUntilReset() {
        Wire wire = newWire(WireType.BINARY);
        AtomicInteger calls = new AtomicInteger();
        wire.contextListener(ContextEvents.class,
                writer -> writer.context(new ContextData("schema", calls.incrementAndGet())));

        final IllegalStateException serializationFailure = new IllegalStateException("payload failed");
        assertEquals(serializationFailure, assertThrows(IllegalStateException.class,
                () -> wire.writeDocument(out -> {
                    out.write("event").text("partial");
                    throw serializationFailure;
                })));
        assertEquals(1, calls.get());

        final IllegalStateException poisoned = assertThrows(IllegalStateException.class,
                () -> wire.writeDocument(out -> out.write("event").text("blocked")));
        assertTrue(poisoned.getCause().getMessage().contains("Application rollback"));
        assertEquals(1, calls.get());

        wire.reset();
        wire.writeDocument(out -> out.write("event").text("after-reset"));
        assertEquals(2, calls.get());
    }

    @Test
    public void noOpListenerInitialisesLazyTextAndYamlWriteContexts() {
        for (WireType wireType : new WireType[]{WireType.TEXT, WireType.YAML}) {
            Wire wire = newWire(wireType);
            AtomicInteger calls = new AtomicInteger();
            wire.contextListener(ContextEvents.class, ignored -> calls.incrementAndGet());

            try (DocumentContext document = wire.writingDocument(false)) {
                document.wire().write("event").text("data");
            }

            assertEquals(wireType.name(), 1, calls.get());
            assertTrue(wireType.name(), wire.writingIsComplete());
        }
    }

    @Test
    public void metadataCannotBypassAFailedListener() {
        Wire wire = newWire(WireType.BINARY);
        final IllegalStateException original = new IllegalStateException("listener failed");
        wire.contextListener(ContextEvents.class, writer -> {
            throw original;
        });

        assertThrows(IllegalStateException.class,
                () -> wire.methodWriter(ContextEvents.class).event(new EventData("failed", 1)));
        final long writePosition = wire.bytes().writePosition();

        final IllegalStateException metadataFailure = assertThrows(IllegalStateException.class,
                () -> wire.writeDocument(true, out -> out.write("metadata").text("blocked")));
        assertEquals(original, metadataFailure.getCause());
        assertEquals(writePosition, wire.bytes().writePosition());
    }

    @Test
    public void incompleteChainedListenerOutputFailsClosedAcrossWires() {
        exerciseChainedListenerCompletion(false, false);
        exerciseChainedListenerCompletion(false, true);
        exerciseChainedListenerCompletion(true, false);
        exerciseChainedListenerCompletion(true, true);
    }

    @Test
    public void listenerRollbackFailsClosedAcrossWires() {
        exerciseListenerRollback(false);
        exerciseListenerRollback(true);
    }

    private void exerciseListenerRollback(boolean proxy) {
        if (proxy) {
            ignoreException("Falling back to proxy method writer");
            System.setProperty(DISABLE_WRITER_PROXY_CODEGEN, "true");
        }
        try {
            for (WireType wireType : WRITABLE_WIRE_TYPES) {
                Wire wire = newWire(wireType);
                AtomicInteger calls = new AtomicInteger();
                wire.contextListener(ChainedContextEvents.class, writer -> {
                    calls.incrementAndGet();
                    writer.context("schema");
                    wire.rollbackIfNotComplete();
                });

                ContextEvents dataWriter = wire.methodWriter(ContextEvents.class);
                final IllegalStateException failure = assertThrows(IllegalStateException.class,
                        () -> dataWriter.event(new EventData("blocked", 1)));
                assertEquals(wireType.name(),
                        "Context listener rolled back its output document",
                        failure.getMessage());
                assertEquals(wireType.name(), 1, calls.get());
                assertThrows(IllegalStateException.class,
                        () -> dataWriter.event(new EventData("still-blocked", 2)));
            }
        } finally {
            if (proxy)
                System.clearProperty(DISABLE_WRITER_PROXY_CODEGEN);
        }
    }

    private void exerciseChainedListenerCompletion(boolean proxy, boolean complete) {
        if (proxy) {
            ignoreException("Falling back to proxy method writer");
            System.setProperty(DISABLE_WRITER_PROXY_CODEGEN, "true");
        }
        try {
            for (WireType wireType : WRITABLE_WIRE_TYPES) {
                Wire wire = newWire(wireType);
                wire.contextListener(ChainedContextEvents.class, writer -> {
                    ChainedContextTail tail = writer.context("schema");
                    if (complete)
                        tail.complete(7);
                });

                ContextEvents dataWriter = wire.methodWriter(ContextEvents.class);
                if (complete) {
                    dataWriter.event(new EventData("data", 1));
                    assertTrue(wireType.name(), wire.writingIsComplete());

                    if (!proxy) {
                        AtomicReference<String> contextName = new AtomicReference<>();
                        AtomicInteger contextVersion = new AtomicInteger(Integer.MIN_VALUE);
                        ChainedContextEvents contextReader = name -> {
                            contextName.set(name);
                            return contextVersion::set;
                        };
                        assertTrue(wireType.name(), wire.methodReader(contextReader).readOne());
                        assertEquals(wireType.name(), "schema", contextName.get());
                        assertEquals(wireType.name(), 7, contextVersion.get());
                    }
                } else {
                    final IllegalStateException failure = assertThrows(IllegalStateException.class,
                            () -> dataWriter.event(new EventData("blocked", 1)));
                    assertEquals(wireType.name(),
                            "Context listener returned with an incomplete chained document",
                            failure.getMessage());
                    assertTrue(wireType.name(), wire.writingIsComplete());
                    assertThrows(IllegalStateException.class,
                            () -> dataWriter.event(new EventData("still-blocked", 2)));
                }
            }
        } finally {
            if (proxy)
                System.clearProperty(DISABLE_WRITER_PROXY_CODEGEN);
        }
    }

    @Test
    public void proxyWriterFreezesItsOutputAtBuildTime() {
        ignoreException("Falling back to proxy method writer");
        System.setProperty(DISABLE_WRITER_PROXY_CODEGEN, "true");
        try {
            Wire first = newWire(WireType.BINARY);
            Wire second = newWire(WireType.BINARY);
            VanillaMethodWriterBuilder<ContextEvents> builder =
                    (VanillaMethodWriterBuilder<ContextEvents>) first.methodWriterBuilder(ContextEvents.class);
            ContextEvents writer = builder.build();
            builder.marshallableOut(second);

            writer.event(new EventData("first", 1));

            assertTrue(first.bytes().writePosition() > 0);
            assertEquals(0, second.bytes().writePosition());
        } finally {
            System.clearProperty(DISABLE_WRITER_PROXY_CODEGEN);
        }
    }

    @Test
    public void explicitProxyClassFreezesItsOutputAtBuildTime() {
        Wire first = newWire(WireType.BINARY);
        Wire second = newWire(WireType.BINARY);
        VanillaMethodWriterBuilder<ContextEvents> builder =
                (VanillaMethodWriterBuilder<ContextEvents>) first.methodWriterBuilder(ContextEvents.class);
        builder.proxyClass(PrecompiledContextEvents.class);
        ContextEvents writer = builder.build();
        builder.marshallableOut(second);

        writer.event(new EventData("first", 1));

        assertTrue(first.bytes().writePosition() > 0);
        assertEquals(0, second.bytes().writePosition());
    }

    @Test
    public void copyWithPreservesWriterOptions() {
        MethodWriterInvocationHandler replacement = createMock(MethodWriterInvocationHandler.class);
        Closeable closeable = createMock(Closeable.class);
        replacement.genericEvent("event");
        replacement.onClose(closeable);
        replacement.recordHistory(true);
        replacement.useMethodIds(false);
        replay(replacement, closeable);

        MethodWriterInvocationHandlerSupplier original =
                new MethodWriterInvocationHandlerSupplier(() -> {
                    throw new AssertionError("the original output must not be resolved");
                });
        original.recordHistory(true);
        original.onClose(closeable);
        original.disableThreadSafe(true);
        original.genericEvent("event");
        original.useMethodIds(false);

        AtomicInteger replacementCalls = new AtomicInteger();
        MethodWriterInvocationHandlerSupplier copy = original.copyWith(() -> {
            replacementCalls.incrementAndGet();
            return replacement;
        });

        assertSame(replacement, copy.get());
        assertSame(replacement, copy.get());
        assertEquals(1, replacementCalls.get());
        verify(replacement, closeable);
    }

    @Test
    public void legacySupplierConstructorBuildsProxyWriter() throws Throwable {
        ignoreException("Falling back to proxy method writer");
        System.setProperty(DISABLE_WRITER_PROXY_CODEGEN, "true");
        try {
            MethodWriterInvocationHandler handler = createMock(MethodWriterInvocationHandler.class);
            handler.genericEvent(null);
            handler.onClose(null);
            handler.recordHistory(false);
            handler.useMethodIds(true);
            expect(handler.invoke(anyObject(), anyObject(Method.class), aryEq(new Object[]{"value"})))
                    .andReturn(null);
            replay(handler);

            VanillaMethodWriterBuilder<LegacyEvent> builder = new VanillaMethodWriterBuilder<>(
                    LegacyEvent.class, WireType.TEXT, () -> handler);
            builder.marshallableOut(newWire(WireType.TEXT));
            builder.build().event("value");

            verify(handler);
        } finally {
            System.clearProperty(DISABLE_WRITER_PROXY_CODEGEN);
        }
    }

    @Test
    public void allConcreteWiresInvokeListenerBeforeFirstDataDocument() {
        for (WireType wireType : WRITABLE_WIRE_TYPES) {
            Wire wire = newWire(wireType);
            AtomicInteger calls = new AtomicInteger();
            wire.contextListener(ContextEvents.class, writer -> {
                calls.incrementAndGet();
                writer.context(new ContextData(wireType.name(), 1));
            });

            assertEquals(wireType.name(), 0, calls.get());
            try (DocumentContext dc = wire.acquireWritingDocument(false)) {
                assertEquals(wireType.name(), 1, calls.get());
                dc.wire().write("value").text("one");
            }
            try (DocumentContext dc = wire.writingDocument(false)) {
                dc.wire().write("value").text("two");
            }

            wire.reset();
            try (DocumentContext dc = wire.writingDocument(false)) {
                dc.wire().write("value").text("three");
            }

            assertEquals(wireType.name(), 2, calls.get());
        }
    }

    @Test
    public void proxyFallbackUsesTheSuppliedListenerOutput() {
        ignoreException("Falling back to proxy method writer");
        System.setProperty(DISABLE_WRITER_PROXY_CODEGEN, "true");
        try {
            for (WireType wireType : WRITABLE_WIRE_TYPES) {
                Wire wire = newWire(wireType);
                AtomicInteger calls = new AtomicInteger();
                wire.contextListener(ContextEvents.class, writer -> {
                    calls.incrementAndGet();
                    writer.context(new ContextData(wireType.name(), 1));
                });

                wire.methodWriter(ContextEvents.class).event(new EventData("data", 1));

                assertEquals(wireType.name(), 1, calls.get());
            }
        } finally {
            System.clearProperty(DISABLE_WRITER_PROXY_CODEGEN);
        }
    }

    @Test
    public void clearRetainsTheCurrentOutputContext() {
        for (WireType wireType : WRITABLE_WIRE_TYPES) {
            Wire wire = newWire(wireType);
            AtomicInteger calls = new AtomicInteger();
            wire.contextListener(ContextEvents.class, writer -> {
                calls.incrementAndGet();
                writer.context(new ContextData(wireType.name(), 1));
            });

            ContextEvents writer = wire.methodWriter(ContextEvents.class);
            writer.event(new EventData("before", 1));
            wire.clear();
            writer.event(new EventData("after", 2));

            assertEquals(wireType.name(), 1, calls.get());
        }
    }

    @Test
    public void resetReusesListenerForTheNextOutputContext() {
        Wire wire = newWire(WireType.BINARY);
        AtomicInteger calls = new AtomicInteger();
        wire.contextListener(ContextEvents.class,
                writer -> writer.context(new ContextData("schema", calls.incrementAndGet())));

        ContextEvents writer = wire.methodWriter(ContextEvents.class);
        writer.event(new EventData("before", 1));

        wire.reset();
        writer.event(new EventData("after", 2));

        assertEquals(2, calls.get());
        assertThrows(IllegalStateException.class,
                () -> wire.contextListener(ContextEvents.class, ignored -> {
                }));
        assertEquals("" +
                        "--- !!data #binary\n" +
                        "context: {\n" +
                        "  name: schema,\n" +
                        "  version: 2\n" +
                        "}\n" +
                        "# position: 40, header: 1\n" +
                        "--- !!data #binary\n" +
                        "event: {\n" +
                        "  name: after,\n" +
                        "  sequence: 2\n" +
                        "}\n",
                Wires.fromSizePrefixedBlobs(wire));
    }

    @Test
    public void suppliedMethodWriterCanBeRetainedAfterNotification() {
        Wire wire = newWire(WireType.BINARY);
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<ContextEvents> suppliedWriter = new AtomicReference<>();
        wire.contextListener(ContextEvents.class, writer -> {
            calls.incrementAndGet();
            suppliedWriter.set(writer);
            writer.context(new ContextData("schema", 7));
        });

        ContextEvents writer = wire.methodWriter(ContextEvents.class);
        writer.event(new EventData("one", 1));
        suppliedWriter.get().event(new EventData("retained", 2));

        assertEquals(1, calls.get());
        assertEquals("" +
                        "--- !!data #binary\n" +
                        "context: {\n" +
                        "  name: schema,\n" +
                        "  version: 7\n" +
                        "}\n" +
                        "# position: 40, header: 1\n" +
                        "--- !!data #binary\n" +
                        "event: {\n" +
                        "  name: one,\n" +
                        "  sequence: 1\n" +
                        "}\n" +
                        "# position: 76, header: 2\n" +
                        "--- !!data #binary\n" +
                        "event: {\n" +
                        "  name: retained,\n" +
                        "  sequence: 2\n" +
                        "}\n",
                Wires.fromSizePrefixedBlobs(wire));
    }

    @Test
    public void retainedSuppliedWriterCanStartNextContextAfterReset() {
        Wire wire = newWire(WireType.BINARY);
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<ContextEvents> suppliedWriter = new AtomicReference<>();
        wire.contextListener(ContextEvents.class, writer -> {
            suppliedWriter.set(writer);
            writer.context(new ContextData("schema", calls.incrementAndGet()));
        });

        ContextEvents applicationWriter = wire.methodWriter(ContextEvents.class);
        applicationWriter.event(new EventData("before-reset", 1));
        ContextEvents retainedWriter = suppliedWriter.get();
        assertEquals(1, calls.get());

        wire.reset();

        retainedWriter.event(new EventData("retained-after-reset", 2));
        applicationWriter.event(new EventData("ordinary-after-retained", 3));

        assertEquals(2, calls.get());
        String dump = Wires.fromSizePrefixedBlobs(wire);
        assertTrue(dump, dump.indexOf("version: 2") < dump.indexOf("name: retained-after-reset"));
        assertTrue(dump, dump.indexOf("name: retained-after-reset") < dump.indexOf("name: ordinary-after-retained"));
    }

    @Test
    public void listenerFreeWireCannotBeConfiguredAfterFirstUse() {
        Wire wire = newWire(WireType.BINARY);
        try (DocumentContext dc = wire.writingDocument(false)) {
            dc.wire().write("event").text("first");
        }

        assertThrows(IllegalStateException.class,
                () -> wire.contextListener(ContextEvents.class, ignored -> {
                }));
    }

    @Test
    public void directWriteEntryPointsInvokeListenerOnce() {
        Wire completeWire = newWire(WireType.BINARY);
        AtomicInteger completeCalls = new AtomicInteger();
        completeWire.contextListener(ContextEvents.class, ignored -> completeCalls.incrementAndGet());

        completeWire.writeDocument(true, wire -> wire.write("header").text("metadata"));
        assertEquals(0, completeCalls.get());
        completeWire.writeDocument(false, wire -> wire.write("event").text("complete"));
        completeWire.writeDocument(false, wire -> wire.write("event").text("second"));

        Wire incompleteWire = newWire(WireType.BINARY);
        AtomicInteger incompleteCalls = new AtomicInteger();
        incompleteWire.contextListener(ContextEvents.class, ignored -> incompleteCalls.incrementAndGet());

        incompleteWire.writeNotCompleteDocument(false,
                wire -> wire.write("event").text("incomplete"));

        Wire incompleteMetadataWire = newWire(WireType.BINARY);
        AtomicInteger incompleteMetadataCalls = new AtomicInteger();
        incompleteMetadataWire.contextListener(ContextEvents.class,
                ignored -> incompleteMetadataCalls.incrementAndGet());
        incompleteMetadataWire.writeNotCompleteDocument(true,
                wire -> wire.write("header").text("metadata"));

        assertEquals(1, completeCalls.get());
        assertEquals(1, incompleteCalls.get());
        assertEquals(0, incompleteMetadataCalls.get());
    }

    @Test
    public void lazyTextualWiresCanNotifyBeforeTheirFirstWriteContextExists() {
        final Bytes<?> textBytes = Bytes.allocateElasticOnHeap();
        final Bytes<?> yamlBytes = Bytes.allocateElasticOnHeap();
        final Bytes<?> jsonBytes = Bytes.allocateElasticOnHeap();
        allocatedBytes.add(textBytes);
        allocatedBytes.add(yamlBytes);
        allocatedBytes.add(jsonBytes);

        final Wire[] wires = {
                new TextWire(textBytes),
                new YamlWire(yamlBytes),
                new JSONWire(jsonBytes)
        };
        for (Wire wire : wires) {
            final AtomicInteger calls = new AtomicInteger();
            wire.contextListener(ContextEvents.class, ignored -> calls.incrementAndGet());

            wire.writeDocument(false, out -> out.write("event").text("first"));

            assertEquals(wire.getClass().getSimpleName(), 1, calls.get());
            assertTrue(wire.getClass().getSimpleName(), wire.bytes().writePosition() > 0);
        }
    }

    @Test
    public void rollbackBeforeFirstTextOrYamlDocumentIsHarmless() {
        final Bytes<?> textBytes = Bytes.allocateElasticOnHeap();
        final Bytes<?> yamlBytes = Bytes.allocateElasticOnHeap();
        allocatedBytes.add(textBytes);
        allocatedBytes.add(yamlBytes);

        for (Wire wire : new Wire[]{new TextWire(textBytes), new YamlWire(yamlBytes)}) {
            wire.rollbackIfNotComplete();

            assertTrue(wire.getClass().getSimpleName(), wire.writingIsComplete());
            assertEquals(wire.getClass().getSimpleName(), 0, wire.bytes().writePosition());
        }
    }

    @Test
    public void directWriteFailuresRollbackAndPoisonAcrossWiresAndEntryPoints() {
        for (WireType wireType : WRITABLE_WIRE_TYPES) {
            for (int entryPoint = 0; entryPoint < 3; entryPoint++) {
                final int directEntryPoint = entryPoint;
                Wire wire = newWire(wireType);
                AtomicInteger calls = new AtomicInteger();
                AtomicLong contextBoundary = new AtomicLong(-1);
                wire.contextListener(ContextEvents.class, writer -> {
                    writer.context(new ContextData("schema", calls.incrementAndGet()));
                    contextBoundary.set(wire.bytes().writePosition());
                });

                final IllegalStateException payloadFailure = new IllegalStateException(
                        wireType + " entryPoint=" + entryPoint);
                final long[] payloadPosition = {-1};
                WriteMarshallable failingWriter = out -> {
                    payloadPosition[0] = out.bytes().writePosition();
                    out.bytes().writeByte((byte) 0x5a);
                    throw payloadFailure;
                };

                assertEquals(payloadFailure, assertThrows(IllegalStateException.class,
                        () -> writeDirectly(wire, directEntryPoint, failingWriter)));
                assertEquals(wireType + " entryPoint=" + entryPoint, 1, calls.get());
                assertEquals(wireType + " entryPoint=" + entryPoint,
                        contextBoundary.get(), wire.bytes().writePosition());
                assertTrue(wireType + " entryPoint=" + entryPoint,
                        contextBoundary.get() < payloadPosition[0]);

                final long positionAfterRollback = wire.bytes().writePosition();
                final IllegalStateException poisoned = assertThrows(IllegalStateException.class,
                        () -> wire.writeDocument(false, out -> out.bytes().writeByte((byte) 1)));
                assertTrue(poisoned.getCause().getMessage().contains("Application rollback"));
                assertEquals(positionAfterRollback, wire.bytes().writePosition());
                assertEquals(1, calls.get());
            }
        }
    }

    private static void writeDirectly(Wire wire, int entryPoint, WriteMarshallable writer) {
        switch (entryPoint) {
            case 0:
                wire.writeDocument(false, writer);
                return;
            case 1:
                wire.writeNotCompleteDocument(false, writer);
                return;
            case 2:
                Wires.writeData(wire, writer);
                return;
            default:
                throw new AssertionError(entryPoint);
        }
    }

    @Test
    public void wiresWriteDataUsesLifecycleAndClosesRegistrationWindow() {
        Wire wire = newWire(WireType.BINARY);
        AtomicInteger calls = new AtomicInteger();
        wire.contextListener(ContextEvents.class, writer -> {
            calls.incrementAndGet();
            writer.context(new ContextData("schema", 7));
        });

        Wires.writeData(wire, out -> out.write("event").text("one"));
        Wires.writeData(wire, out -> out.write("event").text("two"));
        WireInternal.writeData(wire, false, false,
                out -> out.write("event").text("three"));

        assertEquals(1, calls.get());
        assertThrows(IllegalStateException.class,
                () -> wire.contextListener(ContextEvents.class, ignored -> {
                }));
    }

    @Test
    public void listenerCannotReenterThroughOuterWire() {
        Wire wire = newWire(WireType.BINARY);
        wire.contextListener(ContextEvents.class, ignored ->
                wire.writeDocument(false, out -> out.write("illegal").text("re-entry")));

        ContextEvents writer = wire.methodWriter(ContextEvents.class);
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> writer.event(new EventData("one", 1)));

        assertEquals("Only the supplied context writer may write while the context listener is running",
                failure.getMessage());
        assertThrows(IllegalStateException.class,
                () -> writer.event(new EventData("two", 2)));
    }

    @Test
    public void listenerCannotClearTheOuterWire() {
        for (WireType wireType : WRITABLE_WIRE_TYPES) {
            Wire wire = newWire(wireType);
            wire.contextListener(ContextEvents.class, ignored -> wire.clear());

            ContextEvents writer = wire.methodWriter(ContextEvents.class);
            IllegalStateException failure = assertThrows(wireType.name(), IllegalStateException.class,
                    () -> writer.event(new EventData("one", 1)));

            assertEquals(wireType.name(),
                    "Cannot reset a wire while its context listener is running", failure.getMessage());
            assertThrows(wireType.name(), IllegalStateException.class,
                    () -> writer.event(new EventData("two", 2)));
        }
    }

    @Test
    public void readAnyRejectsContextListeners() {
        Wire wire = newWire(WireType.READ_ANY);

        assertEquals(MarshallableOut.UNSET_CONTEXT, wire.contextCount());
        assertThrows(UnsupportedOperationException.class,
                () -> wire.contextListener(ContextEvents.class, ignored -> {
                }));
    }

    private Wire newWire(WireType wireType) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        allocatedBytes.add(bytes);
        return wireType.apply(bytes);
    }

    interface ContextEvents {
        void context(ContextData context);

        void event(EventData event);
    }

    interface LegacyEvent {
        void event(String value);
    }

    interface ChainedContextEvents {
        ChainedContextTail context(String name);
    }

    interface ChainedContextTail {
        void complete(int version);
    }

    public static final class PrecompiledContextEvents implements ContextEvents {
        private static final Method CONTEXT;
        private static final Method EVENT;

        static {
            try {
                CONTEXT = ContextEvents.class.getMethod("context", ContextData.class);
                EVENT = ContextEvents.class.getMethod("event", EventData.class);
            } catch (NoSuchMethodException e) {
                throw new AssertionError(e);
            }
        }

        private final MethodWriterInvocationHandlerSupplier handlerSupplier;

        public PrecompiledContextEvents(MethodWriterInvocationHandlerSupplier handlerSupplier) {
            this.handlerSupplier = handlerSupplier;
        }

        @Override
        public void context(ContextData context) {
            invoke(CONTEXT, context);
        }

        @Override
        public void event(EventData event) {
            invoke(EVENT, event);
        }

        private void invoke(Method method, Object argument) {
            try {
                handlerSupplier.get().invoke(this, method, new Object[]{argument});
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable throwable) {
                throw new AssertionError(throwable);
            }
        }
    }

    static final class ContextData extends SelfDescribingMarshallable {
        String name;
        int version;

        ContextData(String name, int version) {
            this.name = name;
            this.version = version;
        }
    }

    static final class EventData extends SelfDescribingMarshallable {
        String name;
        long sequence;

        EventData(String name, long sequence) {
            this.name = name;
            this.sequence = sequence;
        }
    }
}
