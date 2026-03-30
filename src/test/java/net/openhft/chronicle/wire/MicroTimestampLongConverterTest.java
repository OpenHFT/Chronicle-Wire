/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.Test;

import static net.openhft.chronicle.wire.MicroTimestampLongConverter.INSTANCE;
import static org.junit.jupiter.api.Assertions.*;

class MicroTimestampLongConverterTest extends WireTestCommon {

    // Define constant strings for different timestamp representations
    private static final String TIMESTAMP_STRING_UTC = "2023-02-15T05:31:49.856123Z";
    private static final String TIMESTAMP_STRING_UTC_NO_SUFFIX = "2023-02-15T05:31:49.856123";
    private static final long TIMESTAMP = 1676439109856123L;
    private static final String TIMESTAMP_STRING_MELBOURNE = "2023-02-15T16:31:49.856123+11:00";

    // Test Cases
    // Test if the parse method correctly interprets timestamps
    @Test
    void parse() {
        long now = System.currentTimeMillis();
        // Uncomment the below lines if needed
        // long parse1 = INSTANCE.parse(Long.toString(now));
        // assertEquals(now, parse1 / 1000);
        long parse2 = INSTANCE.parse(Long.toString(now * 1000));
        assertEquals(now, parse2 / 1000);
        long parse3 = INSTANCE.parse(INSTANCE.asString(now * 1000));
        assertEquals(now, parse3 / 1000);
    }

    // Test different date format parsing
    @Test
    void parse2() {
        assertEquals(INSTANCE.parse("2020/09/18T01:02:03.456"), INSTANCE.parse("2020-09-18T01:02:03.456"));
        assertEquals(INSTANCE.parse("2020/09/18T01:02:03.456789"), INSTANCE.parse("2020-09-18T01:02:03.456789"));
        assertEquals(INSTANCE.parse("2020/09/18T01:02:03.456789"), INSTANCE.parse("2020-09-18T01:02:03.456789012"));
    }

    // Check if trailing 'Z' in timestamp does not affect parsing
    @Test
    void parse3() {
        assertEquals(INSTANCE.parse("202020/09/18T01:02:03.456789", 2, 28), INSTANCE.parse("2020-09-18T01:02:03.4567890123", 0, 26));
    }

    @Test
    void testTrailingZ() {
        final String text = "2020-09-18T01:02:03.456789";
        assertEquals(INSTANCE.parse(text), INSTANCE.parse(text + "Z"));
    }

    // Test timestamp parsing with New York timezone
    @Test
    void NYparse() {
        MicroTimestampLongConverter mtlc = new MicroTimestampLongConverter("America/New_York");
        long time = INSTANCE.parse("2020/09/18T01:02:03.456789");
        final String str = mtlc.asString(time);
        assertEquals("2020-09-17T21:02:03.456789-04:00", str);
        assertEquals(time, mtlc.parse(str));
    }

    // Verify that timestamps without timezone are treated as local timestamps
    @Test
    void datesWithNoTimezoneAreAssumedToBeLocal() {
        MicroTimestampLongConverter mtlc = new MicroTimestampLongConverter("America/New_York");
        assertEquals(mtlc.parse("2020-09-17T21:02:03.456789-04:00"), mtlc.parse("2020-09-17T21:02:03.456789"));
    }

    // Test if timestamps are correctly appended for Melbourne timezone
    @Test
    void appendTest() {
        final MicroTimestampLongConverter converter = new MicroTimestampLongConverter("Australia/Melbourne");
        StringBuilder builder = new StringBuilder();
        converter.append(builder, TIMESTAMP);
        assertEquals(TIMESTAMP_STRING_MELBOURNE, builder.toString());
    }

    // Test if timestamps are correctly appended for UTC timezone
    @Test
    void appendTestUTC() {
        final MicroTimestampLongConverter converter = new MicroTimestampLongConverter("UTC");
        StringBuilder builder = new StringBuilder();
        converter.append(builder, TIMESTAMP);
        assertEquals(TIMESTAMP_STRING_UTC_NO_SUFFIX, builder.toString());
    }

    // Test round-trip conversion for various timezones
    @Test
    void roundTripTest() {
        roundTrip(TIMESTAMP_STRING_UTC_NO_SUFFIX, TIMESTAMP, new MicroTimestampLongConverter("UTC"));
        roundTrip(TIMESTAMP_STRING_MELBOURNE, TIMESTAMP, new MicroTimestampLongConverter("Australia/Melbourne"));
    }

    // Helper method for round-trip tests
    private void roundTrip(String timestampString, long timestamp, LongConverter longConverter) {
        assertEquals(longConverter.asString(longConverter.parse(timestampString)), timestampString);
        assertEquals(longConverter.parse(longConverter.asString(timestamp)), timestamp);
    }
}
