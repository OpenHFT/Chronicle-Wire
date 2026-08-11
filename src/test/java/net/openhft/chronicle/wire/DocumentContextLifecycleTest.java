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
        // write two docs
        try (DocumentContext dc = w.writingDocument()) {
            assertEquals(1, dc.contextCount());
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
        try (DocumentContext dc = w.writingDocument()) {
            assertEquals(1, dc.contextCount());
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
    public void progressiveContextOnlyNeedsResendingForHigherCounts() {
        SchemaContext context = new SchemaContext("schema-v1");

        assertFalse(context.needsResending(-1));
        assertTrue(context.needsResending(1));
        assertFalse(context.needsResending(1));
        assertFalse(context.needsResending(0));
        assertTrue(context.needsResending(2));
        assertEquals(2, context.writeCount);
    }

    private static int writeAndReadContextCount(Wire wire) {
        try (DocumentContext dc = wire.writingDocument()) {
            return dc.contextCount();
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
