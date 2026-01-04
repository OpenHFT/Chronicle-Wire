/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Issue886Test {
    public static Collection<Object[]> combinations() {
        return Arrays.asList(new Object[][]{
                {WireType.YAML_ONLY},
                {WireType.JSON_ONLY},
        });
    }

    @MethodSource("combinations")
    @DisplayName("Numeric strings parse to numbers for YAML and JSON")
    @ParameterizedTest(name = "{0}")
    void test(WireType wireType) {
        String data = "{\n" +
                "  \"a\": 1.234,\n" +
                "  \"a1\": '1.234'\n" +
                "  \"a2\": \"1.234\"\n" +
                "  \"b\": 128,\n" +
                "  \"b1\": '128'\n" +
                "  \"b2\": \"128\"\n" +
                "}";
        Wire wire = wireType.apply(Bytes.from(data));
        assertEquals(1.234, wire.read(() -> "a").float64(), 0.0,
                "float64 should parse numeric literal for key a");
        assertEquals(1.234, wire.read(() -> "a1").float64(), 0.0,
                "float64 should parse single-quoted numeric string for key a1");
        assertEquals(1.234, wire.read(() -> "a2").float64(), 0.0,
                "float64 should parse double-quoted numeric string for key a2");
        assertEquals(128L, wire.read(() -> "b").int64(),
                "int64 should parse numeric literal for key b");
        assertEquals(128L, wire.read(() -> "b1").int64(),
                "int64 should parse single-quoted numeric string for key b1");
        assertEquals(128L, wire.read(() -> "b2").int64(),
                "int64 should parse double-quoted numeric string for key b2");

        Wire wire2 = wireType.apply(Bytes.from(data));
        assertEquals(1.234f, wire2.read(() -> "a").float32(), 0.0f,
                "float32 should parse numeric literal for key a");
        assertEquals(1.234f, wire2.read(() -> "a1").float32(), 0.0f,
                "float32 should parse single-quoted numeric string for key a1");
        assertEquals(1.234f, wire2.read(() -> "a2").float32(), 0.0f,
                "float32 should parse double-quoted numeric string for key a2");
        assertEquals(128, wire2.read(() -> "b").int32(),
                "int32 should parse numeric literal for key b");
        assertEquals(128, wire2.read(() -> "b1").int32(),
                "int32 should parse single-quoted numeric string for key b1");
        assertEquals(128, wire2.read(() -> "b2").int32(),
                "int32 should parse double-quoted numeric string for key b2");
    }
}
