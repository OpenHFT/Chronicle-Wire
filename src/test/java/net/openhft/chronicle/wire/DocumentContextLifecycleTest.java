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
}
