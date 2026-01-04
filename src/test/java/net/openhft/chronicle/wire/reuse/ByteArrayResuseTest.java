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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * ByteArrayReuseTest extends WireTestCommon to test the reuse of byte arrays during
 * serialization and deserialization in Chronicle Wire.
 */
public class ByteArrayResuseTest extends net.openhft.chronicle.wire.WireTestCommon {

    private static final class RoundTripResult {
        private final String hex;
        private final Data expected;
        private final Data firstRead;
        private final Data secondRead;
        private final byte[] firstBytes;
        private final byte[] secondBytes;

        private RoundTripResult(String hex, Data expected, Data firstRead, Data secondRead, byte[] firstBytes, byte[] secondBytes) {
            this.hex = hex;
            this.expected = expected;
            this.firstRead = firstRead;
            this.secondRead = secondRead;
            this.firstBytes = firstBytes;
            this.secondBytes = secondBytes;
        }
    }

    /**
     * Test method to verify the serialization and deserialization of byte arrays using
     * a self-describing message format. It checks the equality of the serialized data
     * with the expected hexadecimal string.
     */
    @Test
    @DisplayName("Reuses byte arrays in self-describing format")
    public void writeReadBytesArray() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for byte array reuse tests");

        RoundTripResult result = writeReadBytesArrayRoundTrip(true);
        assertEquals("c4 64 61 74 61                                  # data:\n" +
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
                "   08 09 00\n", result.hex, "byte array reuse: hex selfDescribing=true");
        assertEquals(result.expected, result.firstRead, "byte array reuse: first read selfDescribing=true");
        assertEquals(result.expected, result.secondRead, "byte array reuse: second read selfDescribing=true");
        assertSame(result.firstBytes, result.secondBytes, "byte array reuse: array reused selfDescribing=true");
    }

    /**
     * Test method to verify the serialization and deserialization of byte arrays using
     * a binary format. It checks the equality of the serialized data with the expected
     * hexadecimal string.
     */
    @Test
    @DisplayName("Reuses byte arrays in binary format")
    public void writeReadBytesArrayBinary() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for byte array reuse binary tests");

        RoundTripResult result = writeReadBytesArrayRoundTrip(false);
        assertEquals("c4 64 61 74 61                                  # data:\n" +
                "80 16                                           # Data\n" +
                "   d2 02 96 49 00 00 00 00                         # timestamp\n" +
                "   0a 00 00 00 01 02 03 04 05 06 07 08 09 00       # bytes\n" +
                "c4 64 61 74 61                                  # data:\n" +
                "80 16                                           # Data\n" +
                "   d2 02 96 49 00 00 00 00                         # timestamp\n" +
                "   0a 00 00 00 01 02 03 04 05 06 07 08 09 00       # bytes\n", result.hex, "byte array reuse: hex selfDescribing=false");
        assertEquals(result.expected, result.firstRead, "byte array reuse: first read selfDescribing=false");
        assertEquals(result.expected, result.secondRead, "byte array reuse: second read selfDescribing=false");
        assertSame(result.firstBytes, result.secondBytes, "byte array reuse: array reused selfDescribing=false");
    }

    private RoundTripResult writeReadBytesArrayRoundTrip(boolean selfDescribing) {
        Data expected = new Data();
        expected.selfDescribing = selfDescribing;
        expected.timestamp = 1234567890L;
        expected.bytes = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0};

        Wire wire = WireType.BINARY_LIGHT.apply(new HexDumpBytes());
        wire.write("data")
                .object(Data.class, expected);
        wire.write("data").object(Data.class, expected);

        Data data2 = new Data();
        data2.selfDescribing = selfDescribing;
        wire.read("data").object(data2, Data.class);
        Data firstRead = snapshot(data2);
        byte[] bytes2 = data2.bytes;
        wire.read("data").object(data2, Data.class);
        Data secondRead = snapshot(data2);
        return new RoundTripResult(wire.bytes().toHexString(), expected, firstRead, secondRead, bytes2, data2.bytes);
    }

    private static Data snapshot(Data source) {
        Data copy = new Data();
        copy.selfDescribing = source.selfDescribing;
        copy.timestamp = source.timestamp;
        copy.bytes = source.bytes;
        return copy;
    }

    /**
     * Data class extends SelfDescribingMarshallable for testing byte array reuse.
     * It includes a timestamp and a byte array, with custom serialization behavior.
     */
    static class Data extends SelfDescribingMarshallable {
        long timestamp;
        byte[] bytes;
        boolean selfDescribing;

        @Override
        public boolean usesSelfDescribingMessage() {
            return selfDescribing;
        }

        @Override
        public BinaryLengthLength binaryLengthLength() {
            return BinaryLengthLength.LENGTH_8BIT;
        }
    }
}
