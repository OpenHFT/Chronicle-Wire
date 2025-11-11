/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.Test;

import static net.openhft.chronicle.wire.NanoTimestampLongConverter.INSTANCE;
import static org.junit.Assert.assertEquals;

public class NanoLongConverterTest extends WireTestCommon {

    // Test if a nanosecond timestamp and a duration string can be successfully converted into a Data object and back to the original string format.
    @Test
    public void testNano() {
        // Create a string representation of a Data object with a nanosecond timestamp and a duration.
        String in = "!net.openhft.chronicle.wire.NanoLongConverterTest$Data {\n" +
                "  time: 2019-01-20T23:45:11.123456789,\n" +
                "  ttl: PT1H15M\n" +
                "}\n";

        // Convert the string representation into an actual Data object.
        Data data = Marshallable.fromString(in);

        // Assert that the original string and the string representation of the newly created Data object are equal.
        assertEquals(in, data.toString());
    }

    // Test if trailing 'Z' in the timestamp (indicating UTC time) does not affect parsing for nanoseconds.
    @Test
    public void testTrailingZ() {
        final String text = "2019-01-20T23:45:11.123456789";
        assertEquals(INSTANCE.parse(text), INSTANCE.parse(text + "Z"));
    }

    // Define a nested static class, Data, to represent the timestamp and duration in nanoseconds.
    public static class Data extends SelfDescribingMarshallable {
        // The `@LongConversion` annotation tells the deserializer to use the NanoTimestampLongConverter class
        // to convert the timestamp string to a long value representing nanoseconds.
        @LongConversion(NanoTimestampLongConverter.class)
        public long time;

        // Similarly, the `@LongConversion` annotation for ttl indicates that the NanoDurationLongConverter class
        // should be used to convert the duration string into a long value.
        @LongConversion(NanoDurationLongConverter.class)
        public long ttl;
    }
}
