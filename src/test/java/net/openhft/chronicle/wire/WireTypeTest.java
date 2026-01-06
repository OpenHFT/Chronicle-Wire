/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.util.Mocker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WireTypeTest extends WireTestCommon {

    @Test
    @DisplayName("valueOf returns null when the wire reference is absent")
    void valueOfReturnsNullForNullWire() {
        assertNull(WireType.valueOf((Wire) null), "WireType.valueOf should return null for a null wire");
    }

    @Test
    @DisplayName("valueOf detects text, YAML, and JSON wire implementations")
    void valueOfDetectsTextYamlAndJson() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try {
            assertEquals(WireType.TEXT, WireType.valueOf(new TextWire(bytes)),
                    "WireType.valueOf should detect TextWire instances");
            assertEquals(WireType.YAML, WireType.valueOf(new YamlWire(bytes)),
                    "WireType.valueOf should detect YamlWire instances");
            assertEquals(WireType.JSON, WireType.valueOf(new JSONWire(bytes)),
                    "WireType.valueOf should detect JSONWire instances");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("valueOf detects binary, fieldless, and raw wire implementations")
    void valueOfDetectsBinaryAndRaw() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try {
            assertEquals(WireType.BINARY, WireType.valueOf(new BinaryWire(bytes)),
                    "WireType.valueOf should detect BinaryWire instances");
            Wire fieldless = new BinaryWire(bytes, false, false, true, Integer.MAX_VALUE, "binary");
            assertEquals(WireType.FIELDLESS_BINARY, WireType.valueOf(fieldless),
                    "WireType.valueOf should detect fieldless BinaryWire instances");
            assertEquals(WireType.RAW, WireType.valueOf(new RawWire(bytes)),
                    "WireType.valueOf should detect RawWire instances");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("valueOf unwraps ReadAnyWire for text detection")
    void valueOfUnwrapsReadAnyWireForText() {
        Bytes<?> bytes = Bytes.from("abcdefgh");
        try {
            Wire anyWire = new ReadAnyWire(bytes);
            assertEquals(WireType.TEXT, WireType.valueOf(anyWire),
                    "WireType.valueOf should detect text from ReadAnyWire");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("isText is true for text wires and false for binary")
    void isTextReportsCorrectly() {
        assertTrue(WireType.TEXT.isText(), "WireType TEXT should be treated as text");
        assertTrue(WireType.YAML.isText(), "WireType YAML should be treated as text");
        assertTrue(WireType.JSON.isText(), "WireType JSON should be treated as text");
        assertFalse(WireType.BINARY.isText(), "WireType BINARY should not be treated as text");
    }

    @Test
    @DisplayName("fromString rejects empty input values for wire types")
    void fromStringRejectsEmptyInput() {
        assertThrows(IllegalArgumentException.class,
                () -> WireType.BINARY.fromString(Object.class, ""),
                "WireType.fromString should reject empty input values");
    }

    @Test
    @DisplayName("valueOf rejects unknown wire implementations during detection")
    void valueOfRejectsUnknownWires() {
        Wire wire = Mocker.ignored(Wire.class);
        assertThrows(IllegalStateException.class, () -> WireType.valueOf(wire),
                "WireType.valueOf should reject unknown wire implementations");
    }
}
