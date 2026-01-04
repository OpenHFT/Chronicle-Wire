/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises number formats and boolean parsing across common wire types.
 */
@SuppressWarnings({"deprecation", "removal"})
class WireNumberFormatsTest extends WireTestCommon {

    @Test
    @DisplayName("Text wire parses numeric and boolean formats")
    void textFormats() {
        String s = "i: +5\n" +
                "j: 00\n" +
                "d: 1e3\n" +
                "b: true\n" +
                "mz: -0.0\n";
        TextWire w = TextWire.from(s);
        assertEquals(5,
                w.read("i").int32(),
                "Text wire should parse +5 for i");
        assertEquals(0,
                w.read("j").int32(),
                "Text wire should parse zero for j");
        assertEquals(1000.0,
                w.read("d").float64(),
                0.0,
                "Text wire should parse exponent for d");
        assertTrue(w.read("b").bool(),
                "Text wire should parse true for b");
        assertEquals(0.0,
                w.read("mz").float64(),
                0.0,
                "Text wire should parse minus zero for mz");
    }

    @Test
    @DisplayName("YAML wire parses numeric and boolean formats")
    void yamlFormats() {
        String s = "i: +7\n" +
                "j: 000\n" +
                "d: 2.5e2\n" +
                "b: false\n" +
                "mz: -0.0\n";
        YamlWire w = YamlWire.from(s);
        assertEquals(7,
                w.read("i").int32(),
                "YAML wire should parse +7 for i");
        assertEquals(0,
                w.read("j").int32(),
                "YAML wire should parse zero for j");
        assertEquals(250.0,
                w.read("d").float64(),
                0.0,
                "YAML wire should parse exponent for d");
        assertFalse(w.read("b").bool(),
                "YAML wire should parse false for b");
        assertEquals(0.0,
                w.read("mz").float64(),
                0.0,
                "YAML wire should parse minus zero for mz");
    }

    @Test
    @DisplayName("Binary wire round trips numeric extremes")
    void binaryExtremesRoundTrip() {
        Wire w = WireType.BINARY.apply(Bytes.allocateElasticOnHeap(256));
        w.write("i32min").int32(Integer.MIN_VALUE);
        w.write("i32max").int32(Integer.MAX_VALUE);
        w.write("i64min").int64(Long.MIN_VALUE + 1);
        w.write("i64max").int64(Long.MAX_VALUE);
        w.write("pi").float64(Math.PI);
        w.write("mz").float64(-0.0);

        assertEquals(Integer.MIN_VALUE,
                w.read("i32min").int32(),
                "Binary wire should round trip i32min");
        assertEquals(Integer.MAX_VALUE,
                w.read("i32max").int32(),
                "Binary wire should round trip i32max");
        assertEquals(Long.MIN_VALUE + 1,
                w.read("i64min").int64(),
                "Binary wire should round trip i64min");
        assertEquals(Long.MAX_VALUE,
                w.read("i64max").int64(),
                "Binary wire should round trip i64max");
        assertEquals(Math.PI,
                w.read("pi").float64(),
                0.0,
                "Binary wire should round trip pi");
        assertEquals(0.0,
                w.read("mz").float64(),
                0.0,
                "Binary wire should round trip minus zero");
    }
}
