/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.FieldGroup;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.internal.BytesFieldInfo;
import net.openhft.chronicle.bytes.util.DecoratedBufferUnderflowException;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

// Test class focusing on the serialization and deserialization of Marshallables with embedded bytes.
class EmbeddedBytesMarshallableTest extends WireTestCommon {

    // Before each test, check the current architecture and skip if it's ARM or Azul Zing.
    @BeforeEach
    void checkArch() {
        assumeFalse(Jvm.isArm() || Jvm.isAzulZing(), "ARM or Azul Zing detected; skip embedded bytes tests");
    }

    // Test clearing and appending new data to the embedded bytes.
    @Test
    @DisplayName("Clears and updates embedded bytes content")
    void testClear() {
        // Register the alias for the class.
        ClassAliasPool.CLASS_ALIASES.addAlias(EBM.class);

        // Initialize and set the value for embedded bytes.
        EBM e1 = new EBM();
        e1.a.append("a12345678");
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        e1.writeMarshallable(bytes);

        // Deserialize the data, clear and append new values.
        EBM e2 = new EBM();
        e2.readMarshallable(bytes);
        e2.a.clear();
        e2.a.append("b0000000");

        // Ensure the deserialized and modified data is as expected.
        Assertions.assertEquals("b0000000", e2.a.toString(),
                "Embedded bytes should reflect updated content");

        // Release the bytes.
        bytes.releaseLast();
    }

    // Test serialization and deserialization with certain expected output.
    @Test
    @DisplayName("Serialises embedded bytes into hex dump")
    void ebm() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory disabled; skip embedded bytes marshalling test");

        // Register the alias for the class.
        ClassAliasPool.CLASS_ALIASES.addAlias(EBM.class);

        // Initialize the embedded bytes object and set its values.
        EBM e1 = new EBM();
        e1.number = Base85LongConverter.INSTANCE.parse("Hello!", 0, 5);
        e1.a.append("a12345678901234567890123456789");
        e1.b.append("a1234567890123456789abc");
        e1.c.append("a1234567890");
        final String expected = "!EBM {\n" +
                "  number: Hello,\n" +
                "  a: a12345678901234567890123456789,\n" +
                "  b: a1234567890123456789abc,\n" +
                "  c: a1234567890\n" +
                "}\n";
        assertEquals(expected, e1.toString(), "EBM toString should match before marshalling");
        Bytes<?> bytes = new HexDumpBytes();
        e1.writeMarshallable(bytes);
        assertEquals("00 80 04 08 00 80 04 08 1e 61 31 32 33 34 35 36\n" +
                "37 38 39 30 31 32 33 34 35 36 37 38 39 30 31 32\n" +
                "33 34 35 36 37 38 39 00 17 61 31 32 33 34 35 36\n" +
                "37 38 39 30 31 32 33 34 35 36 37 38 39 61 62 63\n" +
                "c4 5f 74 4c 00 00 00 00 0b 61 31 32 33 34 35 36\n" +
                "37 38 39 30\n", bytes.toHexString(),
                "Hex dump output should match embedded bytes");
        EBM e2 = new EBM();
        e2.readMarshallable(bytes);
        assertEquals(expected, e2.toString(), "EBM toString should match after unmarshalling");
        assertEquals(e1.number, e2.number, "Number should round-trip for embedded bytes");
        bytes.releaseLast();
    }

    @Test
    @DisplayName("Trivially copyable fields round-trip for embedded bytes")
    void triviallyCopyableFieldsRoundTrip() {
        EBM1 ebm1 = new EBM1();
        ebm1.l0 = 11L;
        ebm1.i0 = 12;
        ebm1.s0 = 13;
        ebm1.b0 = 14;
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        ebm1.writeMarshallable(bytes);
        EBM1 ebm1Copy = new EBM1();
        ebm1Copy.readMarshallable(bytes);
        assertEquals(11L, ebm1Copy.l0, "EBM1 first long value should round-trip");
        assertEquals(12, ebm1Copy.i0, "EBM1 i0 should round-trip");
        assertEquals(13, ebm1Copy.s0, "EBM1 s0 should round-trip");
        assertEquals(14, ebm1Copy.b0, "EBM1 b0 should round-trip");

        bytes.clear();
        EBM2 ebm2 = new EBM2();
        ebm2.l0 = 21L;
        ebm2.l1 = 22L;
        ebm2.i0 = 23;
        ebm2.i1 = 24;
        ebm2.s0 = 25;
        ebm2.s1 = 26;
        ebm2.b0 = 27;
        ebm2.b1 = 28;
        ebm2.writeMarshallable(bytes);
        EBM2 ebm2Copy = new EBM2();
        ebm2Copy.readMarshallable(bytes);
        assertEquals(21L, ebm2Copy.l0, "EBM2 first long value should round-trip");
        assertEquals(22L, ebm2Copy.l1, "EBM2 second long value should round-trip");
        assertEquals(23, ebm2Copy.i0, "EBM2 i0 should round-trip");
        assertEquals(24, ebm2Copy.i1, "EBM2 i1 should round-trip");
        assertEquals(25, ebm2Copy.s0, "EBM2 s0 should round-trip");
        assertEquals(26, ebm2Copy.s1, "EBM2 s1 should round-trip");
        assertEquals(27, ebm2Copy.b0, "EBM2 b0 should round-trip");
        assertEquals(28, ebm2Copy.b1, "EBM2 b1 should round-trip");

        bytes.clear();
        EBM3 ebm3 = new EBM3();
        ebm3.l0 = 31L;
        ebm3.l1 = 32L;
        ebm3.l2 = 33L;
        ebm3.i0 = 34;
        ebm3.i1 = 35;
        ebm3.i2 = 36;
        ebm3.s0 = 37;
        ebm3.s1 = 38;
        ebm3.s2 = 39;
        ebm3.b0 = 40;
        ebm3.b1 = 41;
        ebm3.b2 = 42;
        ebm3.writeMarshallable(bytes);
        EBM3 ebm3Copy = new EBM3();
        ebm3Copy.readMarshallable(bytes);
        assertEquals(31L, ebm3Copy.l0, "EBM3 first long value should round-trip");
        assertEquals(32L, ebm3Copy.l1, "EBM3 second long value should round-trip");
        assertEquals(33L, ebm3Copy.l2, "EBM3 third long value should round-trip");
        assertEquals(34, ebm3Copy.i0, "EBM3 i0 should round-trip");
        assertEquals(35, ebm3Copy.i1, "EBM3 i1 should round-trip");
        assertEquals(36, ebm3Copy.i2, "EBM3 i2 should round-trip");
        assertEquals(37, ebm3Copy.s0, "EBM3 s0 should round-trip");
        assertEquals(38, ebm3Copy.s1, "EBM3 s1 should round-trip");
        assertEquals(39, ebm3Copy.s2, "EBM3 s2 should round-trip");
        assertEquals(40, ebm3Copy.b0, "EBM3 b0 should round-trip");
        assertEquals(41, ebm3Copy.b1, "EBM3 b1 should round-trip");
        assertEquals(42, ebm3Copy.b2, "EBM3 b2 should round-trip");

        bytes.releaseLast();
    }

    // Test deserialization with no data. Expected to throw a DecoratedBufferUnderflowException.
    @Test
    @DisplayName("Empty input buffer should be rejected with underflow exception")
    void noData() {
        assertThrows(DecoratedBufferUnderflowException.class, () -> {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
            EBM ebm = new EBM();
            ebm.readMarshallable(bytes);
        }, "Empty input should trigger buffer underflow");
    }

    // Test deserialization with invalid description (even bit count = 0).
    // Expected to throw an IllegalStateException.
    @Test
    @DisplayName("Rejects invalid description with zero field count")
    void invalidDescription() {
        assertThrows(IllegalStateException.class, () -> {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
            bytes.readLimit(64); // even bit count i.e. 0
            EBM ebm = new EBM();
            ebm.readMarshallable(bytes);
        }, "Invalid description should be rejected for zero field count");
    }

    // Test deserialization with another invalid description scenario where it tries to read more data than available.
    // Expected to throw an IllegalStateException.
    @Test
    @DisplayName("Rejects invalid description with insufficient data")
    void invalidDescription2() {
        assertThrows(IllegalStateException.class, () -> {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
            bytes.append("abcd"); // tries to read too much data.
            bytes.readLimit(64);
            EBM ebm = new EBM();
            ebm.readMarshallable(bytes);
        }, "Invalid description should be rejected when data is insufficient");
    }

    // Test deserialization with yet another invalid description, characterized by both an even bit count
    // and an attempt to read more data than available. Expected to throw an IllegalStateException.
    @Test
    @DisplayName("Rejects invalid description with even bit count")
    void invalidDescription3() {
        assertThrows(IllegalStateException.class, () -> {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
            bytes.append("abce"); // even bit count &&  tries to read too much data.
            bytes.readLimit(64);
            EBM ebm = new EBM();
            ebm.readMarshallable(bytes);
        }, "Invalid description should be rejected for even bit count with insufficient data");
    }

    // Test deserialization with an even bit count description.
    // Expected to throw an IllegalStateException.
    @Test
    @DisplayName("Rejects description with even bit count marker")
    void invalidDescription4() {
        assertThrows(IllegalStateException.class, () -> {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
            bytes.append("3\0\0\0"); // even bit count
            bytes.readLimit(64);
            EBM ebm = new EBM();
            ebm.readMarshallable(bytes);
        }, "Invalid description should be rejected for even bit count marker");
    }

    // Test deserialization with field counts exceeding FIELD_COUNT_LIMIT (256).
    // Expected to throw an IllegalStateException.
    @Test
    @DisplayName("Rejects excessive field counts in description")
    void excessiveFieldCounts() {
        assertThrows(IllegalStateException.class, () -> {
            int desc = (200 << 24) | (200 << 16) | 1;
            int length = 200 * 8 + 200 * 4 + 1;
            Bytes<?> bytes = Bytes.allocateElasticOnHeap(length + 8);
            bytes.writeInt(desc);
            for (int i = 0; i < length; i++)
                bytes.writeByte((byte) 0);
            bytes.readLimit(bytes.writePosition());
            EBM1 ebm1 = new EBM1();
            ebm1.readMarshallable(bytes);
        }, "Excessive field counts should be rejected");
    }

    // A class representing a Marshallable object with fields grouped into embedded Bytes.
    // This class extends SelfDescribingTriviallyCopyable, indicating that it can describe its own serialization format.
    static class EBM extends SelfDescribingTriviallyCopyable {
        static final int DESCRIPTION = BytesFieldInfo.lookup(EBM.class).description();
        static final int LENGTH, START;

        static {
            final int[] range = BytesUtil.triviallyCopyableRange(EBM.class);
            LENGTH = range[1] - range[0];
            START = range[0];
        }

        @FieldGroup("a")
        transient long a0, a1, a2, a3;
        @FieldGroup("b")
        transient long b0, b1, b2;
        @FieldGroup("c")
        transient int c0, c1, c3;
        @LongConversion(Base85LongConverter.class)
        long number;
        final Bytes<?> a = Bytes.forFieldGroup(this, "a");
        final Bytes<?> b = Bytes.forFieldGroup(this, "b");
        final Bytes<?> c = Bytes.forFieldGroup(this, "c");

        // Required overrides for the SelfDescribingTriviallyCopyable class to describe its serialization format.
        @Override
        protected int $description() {
            return DESCRIPTION;
        }

        @Override
        protected int $start() {
            return START;
        }

        @Override
        protected int $length() {
            return LENGTH;
        }
    }

    // EBM1 class represents a marshallable object with fields of various data types.
    // It extends the SelfDescribingTriviallyCopyable class, indicating its self-descriptive serialization capability.
    static class EBM1 extends SelfDescribingTriviallyCopyable {

        // Retrieve and store the description of the EBM1 class from the BytesFieldInfo utility.
        static final int DESCRIPTION = BytesFieldInfo.lookup(EBM1.class).description();
        // Define LENGTH and START constants that describe the start position and length of serializable data.
        static final int LENGTH, START;

        static {
            // Calculate the range of trivially copyable bytes for the EBM1 class.
            final int[] range = BytesUtil.triviallyCopyableRange(EBM1.class);
            LENGTH = range[1] - range[0];
            START = range[0];
        }

        // Fields of the EBM1 class.
        long l0;
        int i0;
        short s0;
        byte b0;

        // Mandatory override that returns the stored description of the EBM1 class.
        @Override
        protected int $description() {
            return DESCRIPTION;
        }

        // Mandatory override that returns the starting byte position for serialization.
        @Override
        protected int $start() {
            return START;
        }

        // Mandatory override that returns the length of the serializable data.
        @Override
        protected int $length() {
            return LENGTH;
        }
    }

    // EBM2 is similar to EBM1 but includes a second set of fields.
    static class EBM2 extends SelfDescribingTriviallyCopyable {

        // Retrieve and store the description of the EBM2 class from the BytesFieldInfo utility.
        static final int DESCRIPTION = BytesFieldInfo.lookup(EBM2.class).description();
        // Define LENGTH and START constants that describe the start position and length of serializable data.
        static final int LENGTH, START;

        static {
            // Calculate the range of trivially copyable bytes for the EBM2 class.
            final int[] range = BytesUtil.triviallyCopyableRange(EBM2.class);
            LENGTH = range[1] - range[0];
            START = range[0];
        }

        // Additional fields for the EBM2 class.
        long l0, l1;
        int i0, i1;
        short s0, s1;
        byte b0, b1;

        // Mandatory override that returns the stored description of the EBM2 class.
        @Override
        protected int $description() {
            return DESCRIPTION;
        }

        // Mandatory override that returns the starting byte position for serialization.
        @Override
        protected int $start() {
            return START;
        }

        // Mandatory override that returns the length of the serializable data.
        @Override
        protected int $length() {
            return LENGTH;
        }
    }

    // EBM3 builds upon EBM2 by adding a third set of fields.
    static class EBM3 extends SelfDescribingTriviallyCopyable {

        // Retrieve and store the description of the EBM3 class from the BytesFieldInfo utility.
        static final int DESCRIPTION = BytesFieldInfo.lookup(EBM3.class).description();
        // Define LENGTH and START constants that describe the start position and length of serializable data.
        static final int LENGTH, START;

        static {
            // Calculate the range of trivially copyable bytes for the EBM3 class.
            final int[] range = BytesUtil.triviallyCopyableRange(EBM3.class);
            LENGTH = range[1] - range[0];
            START = range[0];
        }

        // Additional fields for the EBM3 class.
        long l0, l1, l2;
        int i0, i1, i2;
        short s0, s1, s2;
        byte b0, b1, b2;

        // Mandatory override that returns the stored description of the EBM3 class.
        @Override
        protected int $description() {
            return DESCRIPTION;
        }

        // Mandatory override that returns the starting byte position for serialization.
        @Override
        protected int $start() {
            return START;
        }

        // Mandatory override that returns the length of the serializable data.
        @Override
        protected int $length() {
            return LENGTH;
        }
    }
}
