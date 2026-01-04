/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TextWireEdgeCaseTest extends WireTestCommon {

    @Test
    @DisplayName("Parses numeric formats and escaped text")
    public void parsesVariousNumericFormats() {
        String yaml = "i: +5\nj: 1_000\nhex: 0xFF\nfloat: 1.2e3\nminusZero: -0.0\ntext: \"line1\\nline2\"\n";
        TextWire wire = TextWire.from(yaml);

        assertEquals(5, wire.read("i").int32(), "signed integer should parse with plus prefix");
        assertEquals(1_000, wire.read("j").int32(), "underscored integer should parse correctly");
        assertEquals(0xFF, wire.read("hex").int32(), "hex integer should parse correctly");
        assertEquals(1200.0, wire.read("float").float64(), 0.0, "float exponent should parse correctly");
        assertEquals(0.0, wire.read("minusZero").float64(), 0.0, "minus zero should parse as zero");
        assertEquals("line1\nline2", wire.read("text").text(), "escaped newline should round-trip");
    }

    @Test
    @DisplayName("Writes and reads escaped text values")
    public void writesAndReadsEscapedText() {
        TextWire wire = new TextWire(Bytes.allocateElasticOnHeap());
        wire.write("message").text("needs: \"quoting\"");
        String output = wire.bytes().toString();
        assertTrue(output.contains("needs: \\\"quoting\\\""),
                output + " should contain escaped quotes");
        assertEquals("needs: \"quoting\"", TextWire.from(output).read("message").text(),
                "escaped text should round-trip from output");
    }

    @Test
    @DisplayName("Round-trips text document context with message value")
    public void documentContextRoundTrip() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        TextWire wire = new TextWire(bytes);
        wire.useTextDocuments();
        try (DocumentContext dc = wire.writingDocument()) {
            dc.wire().write("msg").text("hi");
        }

        bytes.readPositionRemaining(0, bytes.writePosition());
        try (DocumentContext dc = wire.readingDocument()) {
            assertEquals("hi", dc.wire().read("msg").text(),
                    "document context should round-trip text message");
        }
    }
}
