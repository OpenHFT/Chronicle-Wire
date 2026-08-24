/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Verifies lifecycle of writingDocument/readingDocument across Binary and Text wires.
 */
public class DocumentContextLifecycleTest extends WireTestCommon {
    private static final WireType[] WRITABLE_WIRE_TYPES = {
            WireType.BINARY,
            WireType.TEXT,
            WireType.YAML,
            WireType.RAW
    };

    @Test
    public void binaryReadWriteAndExhaust() {
        Wire w = WireType.BINARY.apply(Bytes.allocateElasticOnHeap(256));
        assertEquals(0, w.contextCount());
        // write two docs
        try (DocumentContext dc = w.writingDocument()) {
            assertEquals(w.contextCount(), dc.contextCount());
            dc.wire().write("a").int32(1);
        }
        try (DocumentContext dc = w.writingDocument()) {
            dc.wire().write("b").text("two");
        }
        // read both
        try (DocumentContext dc = w.readingDocument()) {
            assertTrue(dc.isPresent());
            assertEquals(1, dc.wire().read("a").int32());
        }
        try (DocumentContext dc = w.readingDocument()) {
            assertTrue(dc.isPresent());
            assertEquals("two", dc.wire().read("b").text());
        }
        // exhausted
        try (DocumentContext dc = w.readingDocument()) {
            assertFalse(dc.isPresent());
        }
        assertTrue(w.writingIsComplete());
    }

    @Test
    public void textUseTextDocumentsLifecycle() {
        Wire w = new TextWire(Bytes.allocateElasticOnHeap(256)).useTextDocuments();
        assertEquals(0, w.contextCount());
        try (DocumentContext dc = w.writingDocument()) {
            assertEquals(w.contextCount(), dc.contextCount());
            dc.wire().write("x").int64(11L);
        }
        try (DocumentContext dc = w.writingDocument(true)) { // meta document allowed
            dc.wire().write("y").text("yy");
        }
        // read two then assert exhausted
        try (DocumentContext dc = w.readingDocument()) {
            assertTrue(dc.isPresent());
            assertEquals(11L, dc.wire().read("x").int64());
        }
        try (DocumentContext dc = w.readingDocument()) {
            assertTrue(dc.isPresent());
            assertEquals("yy", dc.wire().read("y").text());
        }
        try (DocumentContext dc = w.readingDocument()) {
            assertFalse(dc.isPresent());
        }
        assertTrue(w.writingIsComplete());
    }

    @Test
    public void holderAndWrapperDelegateContextCount() {
        DocumentContext delegate = new DocumentContextHolder() {
            @Override
            public int contextCount() {
                return 42;
            }
        };
        DocumentContextHolder holder = new DocumentContextHolder();
        WrappedDocumentContext wrapper = new WrappedDocumentContext(delegate) {
            @Override
            public void reset() {
            }
        };

        assertEquals(-1, holder.contextCount());
        holder.documentContext(delegate);
        assertEquals(42, holder.contextCount());
        assertEquals(42, wrapper.contextCount());
    }

    @Test
    public void noDocumentContextReturnsNegativeContextCount() {
        assertEquals(-1, NoDocumentContext.INSTANCE.contextCount());
    }

    @Test
    public void marshallableOutWithoutContextTrackingReturnsNegativeContextCount() {
        MarshallableOut output = new MarshallableOut() {
            @Override
            public DocumentContext writingDocument(boolean metaData) {
                return NoDocumentContext.INSTANCE;
            }

            @Override
            public DocumentContext acquireWritingDocument(boolean metaData) {
                return NoDocumentContext.INSTANCE;
            }
        };

        assertEquals(-1, output.contextCount());
    }

    @Test
    public void resetAdvancesContextCountWhileClearRetainsIt() {
        for (WireType wireType : WRITABLE_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            try {
                Wire wire = wireType.apply(bytes);
                int first = writeAndReadContextCount(wire);

                wire.clear();
                assertEquals(wireType.name(), first, writeAndReadContextCount(wire));

                wire.reset();
                assertEquals(wireType.name(), first + 1, writeAndReadContextCount(wire));
            } finally {
                bytes.releaseLast();
            }
        }
    }

    @Test
    public void testSeamAcceptsZeroButNotUnavailableCount() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            AbstractWire wire = (AbstractWire) WireType.BINARY.apply(bytes);

            wire.outputContextCountForTesting(0);

            assertEquals(0, wire.contextCount());
            assertThrows(IllegalArgumentException.class, () -> wire.outputContextCountForTesting(-1));
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void resetMakesProgressiveContextEligibleAgain() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            Wire wire = WireType.BINARY.apply(bytes);
            SchemaContext context = new SchemaContext("schema-v1");

            assertTrue(writeContextWhenMissing(wire, context));
            assertFalse(writeContextWhenMissing(wire, context));

            wire.reset();

            assertTrue(writeContextWhenMissing(wire, context));
            assertEquals(2, context.writeCount);
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void resetRejectsContextCountOverflowBeforeMutation() {
        for (WireType wireType : WRITABLE_WIRE_TYPES) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            try {
                AbstractWire wire = (AbstractWire) wireType.apply(bytes);
                wire.outputContextCountForTesting(Integer.MAX_VALUE - 1);
                SchemaContext context = new SchemaContext("schema-v1");

                int penultimate = writePayloadAndReadContextCount(wire, "penultimate");
                assertEquals(wireType.name(), Integer.MAX_VALUE - 1, penultimate);
                assertTrue(wireType.name(), context.needsResending(penultimate));

                wire.reset();

                int last = writePayloadAndReadContextCount(wire, "last");
                assertEquals(wireType.name(), Integer.MAX_VALUE, last);
                assertTrue(wireType.name(), context.needsResending(last));

                byte[] contentsBeforeRejectedReset = bytes.toByteArray();
                long readPositionBeforeRejectedReset = bytes.readPosition();
                long writePositionBeforeRejectedReset = bytes.writePosition();

                IllegalStateException exception = assertThrows(IllegalStateException.class, wire::reset);

                assertEquals(wireType.name(), "Output context count exhausted", exception.getMessage());
                assertEquals(wireType.name(), Integer.MAX_VALUE, wire.contextCount());
                assertArrayEquals(wireType.name(), contentsBeforeRejectedReset, bytes.toByteArray());
                assertEquals(wireType.name(), readPositionBeforeRejectedReset, bytes.readPosition());
                assertEquals(wireType.name(), writePositionBeforeRejectedReset, bytes.writePosition());
                assertEquals(wireType.name(), 2, context.writeCount);
            } finally {
                bytes.releaseLast();
            }
        }
    }

    @Test
    public void progressiveContextOnlyNeedsResendingForHigherCounts() {
        SchemaContext context = new SchemaContext("schema-v1");

        assertFalse(context.needsResending(-1));
        assertTrue(context.needsResending(0));
        assertFalse(context.needsResending(0));
        assertTrue(context.needsResending(1));
        assertFalse(context.needsResending(1));
        assertFalse(context.needsResending(0));
        assertTrue(context.needsResending(2));
        assertEquals(3, context.writeCount);
    }

    private static int writeAndReadContextCount(Wire wire) {
        try (DocumentContext dc = wire.writingDocument()) {
            return dc.contextCount();
        }
    }

    private static int writePayloadAndReadContextCount(Wire wire, String payload) {
        try (DocumentContext dc = wire.writingDocument()) {
            int contextCount = dc.contextCount();
            dc.wire().write("payload").text(payload);
            return contextCount;
        }
    }

    private static boolean writeContextWhenMissing(Wire wire, SchemaContext context) {
        try (DocumentContext dc = wire.writingDocument()) {
            if (!context.needsResending(dc.contextCount()))
                return false;
            dc.wire().write("context").marshallable(context);
            return true;
        }
    }

    static final class SchemaContext extends SelfDescribingMarshallable implements ProgressiveContext {
        private final String schema;
        private transient int lastContextCount = -1;
        private transient int writeCount;

        SchemaContext(String schema) {
            this.schema = schema;
        }

        @Override
        public boolean needsResending(int contextCount) {
            if (contextCount <= lastContextCount)
                return false;
            lastContextCount = contextCount;
            writeCount++;
            return true;
        }
    }

}
