/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

public class BinaryInTextTest extends WireTestCommon {

    // Holds the wire type for each test iteration
    private WireType wireType;

    // Specifies the set of wire types that will be passed to the test constructor
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{WireType.TEXT},
                new Object[]{WireType.YAML});
    }

    // Test for converting binary content from text representation to Bytes
    @SuppressWarnings("rawtypes")
    @ParameterizedTest
    @MethodSource("wireTypes")
    public void testBytesFromText(WireType wireType) {
        this.wireType = wireType;
        Bytes<?> a = wireType.fromString(Bytes.class, "A==");
        assertEquals("A==", a.toString());

        BytesStore<?, ?> a2 = wireType.fromString(BytesStore.class, "A==");
        assertEquals("A==", a2.toString());

        Bytes<?> b = wireType.fromString(Bytes.class, "!!binary BA==");
        assertEquals("00000000 04", b.toHexString().substring(0, 58).trim());

        Bytes<?> b2 = wireType.fromString(Bytes.class, "!!binary A1==");
        assertEquals("00000000 03", b2.toHexString().substring(0, 58).trim());
    }

    // Test to validate reserialization of binary content from text
    @ParameterizedTest
    @MethodSource("wireTypes")
    public void testReserialize(WireType wireType) {
        this.wireType = wireType;
        assumeFalse(Jvm.maxDirectMemory() == 0);

        BIT bit = wireType.fromString(BIT.class, "{\n" +
                "b: !!binary AAAAAAA=,\n" +
                "c: !!binary CCCCCCCC,\n" +
                "}");
        String bitToString = bit.toString();
        // Checks both possible serializations since field order is not guaranteed
        assertTrue(bitToString.equals("!net.openhft.chronicle.wire.BinaryInTextTest$BIT {\n" +
                "  b: !!binary AAAAAAA=,\n" +
                "  c: !!binary CCCCCCCC\n" +
                "}\n") ||
                bitToString.equals("!net.openhft.chronicle.wire.BinaryInTextTest$BIT {\n" +
                        "  c: !!binary CCCCCCCC,\n" +
                        "  b: !!binary AAAAAAA=\n" +
                        "}\n"));
    }

    // Inner class to test serialization and deserialization of binary content in text
    @SuppressWarnings("rawtypes")
    private static class BIT extends SelfDescribingMarshallable {
        Bytes<?> b;
        BytesStore<?, ?> c;
    }
}
