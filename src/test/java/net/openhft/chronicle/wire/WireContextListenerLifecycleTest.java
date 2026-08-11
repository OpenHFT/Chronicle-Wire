/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class WireContextListenerLifecycleTest extends WireTestCommon {
    private static final WireType[] WRITABLE_WIRE_TYPES = {
            WireType.BINARY,
            WireType.TEXT,
            WireType.YAML,
            WireType.RAW
    };

    private final List<Bytes<?>> allocatedBytes = new ArrayList<>();

    @After
    public void releaseBytes() {
        allocatedBytes.forEach(Bytes::releaseLast);
        allocatedBytes.clear();
    }

    @Test
    public void contextListenerWaitsForDataAfterMetadataAndWritesDtoOnce() {
        Wire wire = newWire(WireType.BINARY);
        AtomicInteger calls = new AtomicInteger();
        wire.contextListener(ContextEvents.class, writer -> {
            calls.incrementAndGet();
            writer.context(new ContextData("schema", 7));
        });

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
    public void listenerFailureIsNotRetried() {
        Wire wire = newWire(WireType.BINARY);
        AtomicInteger calls = new AtomicInteger();
        wire.contextListener(ContextEvents.class, writer -> {
            calls.incrementAndGet();
            writer.context(new ContextData("schema", 7));
            throw new IllegalStateException("listener failed");
        });

        ContextEvents writer = wire.methodWriter(ContextEvents.class);
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> writer.event(new EventData("failed", 1)));
        assertEquals("listener failed", thrown.getMessage());

        writer.event(new EventData("after", 2));

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
                        "  name: after,\n" +
                        "  sequence: 2\n" +
                        "}\n",
                Wires.fromSizePrefixedBlobs(wire));
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
    public void readAnyRejectsContextListeners() {
        Wire wire = newWire(WireType.READ_ANY);

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
