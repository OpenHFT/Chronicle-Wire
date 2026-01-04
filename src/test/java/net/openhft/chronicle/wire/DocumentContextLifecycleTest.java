/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies lifecycle of writingDocument/readingDocument across Binary and Text wires.
 */
public class DocumentContextLifecycleTest extends WireTestCommon {

    @Test
    @DisplayName("Reads and exhausts documents in binary wire")
    public void binaryReadWriteAndExhaust() {
        Wire w = WireType.BINARY.apply(Bytes.allocateElasticOnHeap(256));
        // write two docs
        try (DocumentContext dc = w.writingDocument()) {
            dc.wire().write("a").int32(1);
        }
        try (DocumentContext dc = w.writingDocument()) {
            dc.wire().write("b").text("two");
        }
        // read both
        try (DocumentContext dc = w.readingDocument()) {
            assertTrue(dc.isPresent(), "first binary document should be present");
            assertEquals(1, dc.wire().read("a").int32(),
                    "first binary document should contain a=1");
        }
        try (DocumentContext dc = w.readingDocument()) {
            assertTrue(dc.isPresent(), "second binary document should be present");
            assertEquals("two", dc.wire().read("b").text(),
                    "second binary document should contain b=two");
        }
        // exhausted
        try (DocumentContext dc = w.readingDocument()) {
            assertFalse(dc.isPresent(), "binary document reader should be exhausted");
        }
        assertTrue(w.writingIsComplete(), "binary writing should be complete after reads");
    }

    @Test
    @DisplayName("Reads and exhausts documents in text wire")
    public void textUseTextDocumentsLifecycle() {
        Wire w = new TextWire(Bytes.allocateElasticOnHeap(256)).useTextDocuments();
        try (DocumentContext dc = w.writingDocument()) {
            dc.wire().write("x").int64(11L);
        }
        try (DocumentContext dc = w.writingDocument(true)) { // meta document allowed
            dc.wire().write("y").text("yy");
        }
        // read two then assert exhausted
        try (DocumentContext dc = w.readingDocument()) {
            assertTrue(dc.isPresent(), "first text document should be present");
            assertEquals(11L, dc.wire().read("x").int64(),
                    "first text document should contain x=11");
        }
        try (DocumentContext dc = w.readingDocument()) {
            assertTrue(dc.isPresent(), "second text document should be present");
            assertEquals("yy", dc.wire().read("y").text(),
                    "second text document should contain y=yy");
        }
        try (DocumentContext dc = w.readingDocument()) {
            assertFalse(dc.isPresent(), "text document reader should be exhausted");
        }
        assertTrue(w.writingIsComplete(), "text writing should be complete after reads");
    }

    @Test
    @DisplayName("Rollback should keep document available for reread")
    public void rollbackKeepsDocumentAvailableForNextRead() {
        Wire w = WireType.BINARY.apply(Bytes.allocateElasticOnHeap(256));
        try (DocumentContext dc = w.writingDocument()) {
            dc.wire().write("item").text("value");
        }

        try (DocumentContext dc = w.readingDocument()) {
            assertTrue(dc.isPresent(), "document should be present before rollback");
            dc.rollbackOnClose();
        }

        try (DocumentContext dc = w.readingDocument()) {
            assertTrue(dc.isPresent(), "document should still be present after rollback");
            assertEquals("value", dc.wire().read("item").text(),
                    "rolled back document should be readable again");
        }

        try (DocumentContext dc = w.readingDocument()) {
            assertFalse(dc.isPresent(), "document reader should be exhausted after replay");
        }
        w.bytes().releaseLast();
    }
}
