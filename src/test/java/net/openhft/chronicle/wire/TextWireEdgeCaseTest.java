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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TextWireEdgeCaseTest extends WireTestCommon {

    @Test
    public void parsesVariousNumericFormats() {
        String yaml = "i: +5\nj: 1_000\nhex: 0xFF\nfloat: 1.2e3\nminusZero: -0.0\ntext: \"line1\\nline2\"\n";
        TextWire wire = TextWire.from(yaml);

        assertEquals(5, wire.read("i").int32());
        assertEquals(1_000, wire.read("j").int32());
        assertEquals(0xFF, wire.read("hex").int32());
        assertEquals(1200.0, wire.read("float").float64(), 0.0);
        assertEquals(0.0, wire.read("minusZero").float64(), 0.0);
        assertEquals("line1\nline2", wire.read("text").text());
    }

    @Test
    public void writesAndReadsEscapedText() {
        TextWire wire = new TextWire(Bytes.allocateElasticOnHeap());
        wire.write("message").text("needs: \"quoting\"");
        String output = wire.bytes().toString();
        assertTrue(output.contains("needs: \\\"quoting\\\""));
        assertEquals("needs: \"quoting\"", TextWire.from(output).read("message").text());
    }

    @Test
    public void documentContextRoundTrip() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        TextWire wire = new TextWire(bytes);
        wire.useTextDocuments();
        try (DocumentContext dc = wire.writingDocument()) {
            dc.wire().write("msg").text("hi");
        }

        bytes.readPositionRemaining(0, bytes.writePosition());
        try (DocumentContext dc = wire.readingDocument()) {
            assertEquals("hi", dc.wire().read("msg").text());
        }
    }
}
