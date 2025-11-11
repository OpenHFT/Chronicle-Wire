/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.util.BinaryLengthLength;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assume.assumeFalse;

/**
 * ByteArrayReuseTest extends WireTestCommon to test the reuse of byte arrays during
 * serialization and deserialization in Chronicle Wire.
 */
public class ByteArrayResuseTest extends net.openhft.chronicle.wire.WireTestCommon {

    /**
     * Test method to verify the serialization and deserialization of byte arrays using
     * a self-describing message format. It checks the equality of the serialized data
     * with the expected hexadecimal string.
     */
    @Test
    public void writeReadBytesArray() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        SELF_DESCRIBING = true;
        doWriteReadBytesArray("" +
                "c4 64 61 74 61                                  # data:\n" +
                "80 22                                           # Data\n" +
                "   c9 74 69 6d 65 73 74 61 6d 70                   # timestamp:\n" +
                "   a6 d2 02 96 49                                  # 1234567890\n" +
                "   c5 62 79 74 65 73 80 0b 8a 01 02 03 04 05 06 07 # bytes:\n" +
                "   08 09 00\n" +
                "c4 64 61 74 61                                  # data:\n" +
                "80 22                                           # Data\n" +
                "   c9 74 69 6d 65 73 74 61 6d 70                   # timestamp:\n" +
                "   a6 d2 02 96 49                                  # 1234567890\n" +
                "   c5 62 79 74 65 73 80 0b 8a 01 02 03 04 05 06 07 # bytes:\n" +
                "   08 09 00\n");
    }           // Provide the expected hex string for the serialized data

    /**
     * Test method to verify the serialization and deserialization of byte arrays using
     * a binary format. It checks the equality of the serialized data with the expected
     * hexadecimal string.
     */
    @Test
    public void writeReadBytesArrayBinary() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        SELF_DESCRIBING = false;
        doWriteReadBytesArray("" +
                "c4 64 61 74 61                                  # data:\n" +
                "80 16                                           # Data\n" +
                "   d2 02 96 49 00 00 00 00                         # timestamp\n" +
                "   0a 00 00 00 01 02 03 04 05 06 07 08 09 00       # bytes\n" +
                "c4 64 61 74 61                                  # data:\n" +
                "80 16                                           # Data\n" +
                "   d2 02 96 49 00 00 00 00                         # timestamp\n" +
                "   0a 00 00 00 01 02 03 04 05 06 07 08 09 00       # bytes\n");
    }           // Provide the expected hex string for the serialized data

    /**
     * Helper method to perform serialization and deserialization of Data objects.
     * It asserts the equality of the serialized data with the expected string and
     * checks object equality and array reference equality after deserialization.
     *
     * @param expected The expected hexadecimal string representation of the serialized data.
     */
    private void doWriteReadBytesArray(String expected) {
        Data data = new Data();
        data.timestamp = 1234567890L;
        byte[] bytes = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0};
        data.bytes = bytes;

        Wire wire = WireType.BINARY_LIGHT.apply(new HexDumpBytes());
        wire.write("data")
                .object(Data.class, data);
        wire.write("data").object(Data.class, data);

        assertEquals(expected,
                wire.bytes().toHexString());

        Data data2 = new Data();
        wire.read("data").object(data2, Data.class);
        assertEquals(data, data2);
        byte[] bytes2 = data2.bytes;
        wire.read("data").object(data2, Data.class);
        assertEquals(data, data2);
        assertSame(bytes2, data2.bytes); // Check for byte array reuse
    }

    private static boolean SELF_DESCRIBING;

    /**
     * Data class extends SelfDescribingMarshallable for testing byte array reuse.
     * It includes a timestamp and a byte array, with custom serialization behavior.
     */
    static class Data extends SelfDescribingMarshallable {
        long timestamp;
        byte[] bytes;

        @Override
        public boolean usesSelfDescribingMessage() {
            return SELF_DESCRIBING;
        }

        @Override
        public BinaryLengthLength binaryLengthLength() {
            return BinaryLengthLength.LENGTH_8BIT;
        }
    }
}
