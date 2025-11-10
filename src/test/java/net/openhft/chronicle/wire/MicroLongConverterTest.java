//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * This class tests the functionality of the MicroLongConverter.
 * It extends the WireTestCommon for common test setup and utilities.
 */
public class MicroLongConverterTest extends WireTestCommon {

    /**
     * This test ensures that MicroLongConverter properly converts microsecond timestamp and duration representations.
     */
    @Test
    public void testMicro() {
        String in = "!net.openhft.chronicle.wire.MicroLongConverterTest$Data {\n" +
                "  time: 2019-01-20T23:45:11.123456,\n" +
                "  ttl: PT1H15M\n" +
                "}\n";
        // Deserialize the provided string into a Data object
        Data data = Marshallable.fromString(in);
        // Ensure the object's string representation matches the original input
        assertEquals(in, data.toString());
    }

    /**
     * Data class with fields representing time and duration, both using custom LongConverters.
     */
    public static class Data extends SelfDescribingMarshallable {
        @LongConversion(MicroTimestampLongConverter.class)
        public long time;  // Timestamp in microseconds

        @LongConversion(MicroDurationLongConverter.class)
        public long ttl;   // Duration in microseconds
    }
}
