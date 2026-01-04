/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class BinaryInTextTest extends WireTestCommon {

    // Specifies the set of wire types that will be passed to the test constructor
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{WireType.TEXT},
                new Object[]{WireType.YAML});
    }

    // Test for converting binary content from text representation to Bytes
    @ParameterizedTest(name = "{0}")
    @MethodSource("wireTypes")
    @SuppressWarnings("rawtypes")
    @DisplayName("Parses base64 bytes from text wire")
    void testBytesFromText(WireType wireType) {
        Bytes<?> a = wireType.fromString(Bytes.class, "A==");
        assertEquals("A==", a.toString(), "Base64 payload should round-trip for wireType=" + wireType);

        BytesStore<?, ?> a2 = wireType.fromString(BytesStore.class, "A==");
        assertEquals("A==", a2.toString(),
                "BytesStore base64 payload should round-trip for wireType=" + wireType);

        Bytes<?> b = wireType.fromString(Bytes.class, "!!binary BA==");
        assertEquals("00000000 04", b.toHexString().substring(0, 58).trim(),
                "Hex prefix for BA== should match for wireType=" + wireType);

        Bytes<?> b2 = wireType.fromString(Bytes.class, "!!binary A1==");
        assertEquals("00000000 03", b2.toHexString().substring(0, 58).trim(),
                "Hex prefix for A1== should match for wireType=" + wireType);
    }

    // Test to validate reserialization of binary content from text
    @ParameterizedTest(name = "{0}")
    @MethodSource("wireTypes")
    @DisplayName("Reserialises binary fields from text input")
    void testReserialize(WireType wireType) {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory disabled; skip reserialise test");

        BIT bit = wireType.fromString(BIT.class, "{\n" +
                "b: !!binary AAAAAAA=,\n" +
                "c: !!binary CCCCCCCC,\n" +
                "}");
        assertNotNull(bit.b, "Field b should be populated for wireType=" + wireType);
        assertNotNull(bit.c, "Field c should be populated for wireType=" + wireType);
        String bitToString = bit.toString();
        // Checks both possible serializations since field order is not guaranteed
        assertTrue(bitToString.equals("!net.openhft.chronicle.wire.BinaryInTextTest$BIT {\n" +
                "  b: !!binary AAAAAAA=,\n" +
                "  c: !!binary CCCCCCCC\n" +
                "}\n") ||
                bitToString.equals("!net.openhft.chronicle.wire.BinaryInTextTest$BIT {\n" +
                        "  c: !!binary CCCCCCCC,\n" +
                        "  b: !!binary AAAAAAA=\n" +
                        "}\n"),
                "BIT should serialise with fields b and c; got: " + bitToString);
    }

    // Inner class to test serialization and deserialization of binary content in text
    @SuppressWarnings("rawtypes")
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    private static class BIT extends SelfDescribingMarshallable {
        Bytes<?> b;
        BytesStore<?, ?> c;
    }
}
