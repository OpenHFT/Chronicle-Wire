/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        TextWire w = (TextWire) new TextWire(Bytes.allocateElasticOnHeap(256)).useTextDocuments();
        try (DocumentContext dc = w.writingDocument()) {
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

