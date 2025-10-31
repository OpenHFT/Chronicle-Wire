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
 * Light checks for string interning via WireInternal and simple dump content.
 */
public class WireInternerAndDumpTest extends WireTestCommon {

    @Test
    public void textWireInternsRepeatedStrings() {
        TextWire w = new TextWire(Bytes.allocateElasticOnHeap(256)).useTextDocuments();
        try (DocumentContext dc = w.writingDocument()) {
            dc.wire().write("k").text("alpha");
        }
        try (DocumentContext dc = w.writingDocument()) {
            dc.wire().write("k").text("alpha");
        }
        String s1, s2;
        try (DocumentContext dc = w.readingDocument()) {
            s1 = dc.wire().read("k").text();
        }
        try (DocumentContext dc = w.readingDocument()) {
            s2 = dc.wire().read("k").text();
        }
        // Same canonical instance expected due to interning
        assertSame(s1, s2);
        // Do not assert on global interner counts as other tests may have already
        // populated the interner; asserting referential equality is sufficient.
    }

    @Test
    public void binaryWireHexDumpContainsKeys() {
        Wire w = WireType.BINARY.apply(Bytes.allocateElasticOnHeap(128));
        try (DocumentContext dc = w.writingDocument()) {
            dc.wire().write("foo").int32(42);
        }
        String hex = w.bytes().toHexString();
        assertTrue(hex.contains("foo")); // dumped as ASCII alongside hex
    }
}
