/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.Test;

import static net.openhft.chronicle.wire.MilliTimestampLongConverter.INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"deprecation", "removal"})
public class MilliTimestampLongConverterTest extends WireTestCommon {

    // Define constant strings for different timestamp representations
    private static final String TIMESTAMP_STRING_UTC = "2023-02-15T05:31:49.856Z";
    private static final String TIMESTAMP_STRING_UTC_NO_SUFFIX = "2023-02-15T05:31:49.856";
    private static final long TIMESTAMP = 1676439109856L;
    private static final String TIMESTAMP_STRING_MELBOURNE = "2023-02-15T16:31:49.856+11:00";

    // Test Cases
    // Test the parsing functionality of the converter with current time
    @Test
    public void parse() {
        long now = System.currentTimeMillis();

        // Parse the current timestamp from its string representation and verify equality
        long parse1 = INSTANCE.parse(Long.toString(now));
        assertEquals(now, parse1);
        long parse2 = INSTANCE.parse(Long.toString(now));
        assertEquals(now, parse2);
        String text = INSTANCE.asString(now);
        long parse3 = INSTANCE.parse(text);
        assertEquals(now, parse3);
    }

    // Test different date format parsing
    @Test
    public void parse2() {
        assertEquals(INSTANCE.parse("2020/09/18T01:02:03.456"),
                INSTANCE.parse("2020-09-18T01:02:03.456"));
        assertEquals(INSTANCE.parse("2020/09/18T01:02:03.456"),
                INSTANCE.parse("2020-09-18T01:02:03.456789"));
        assertEquals(INSTANCE.parse("2020/09/18T01:02:03.456"),
                INSTANCE.parse("2020-09-18T01:02:03.456789012"));
    }

    // Test if trailing 'Z' in the timestamp does not affect parsing
    @Test
    public void parse3() {
        assertEquals(INSTANCE.parse("202020/09/18T01:02:03.456", 2, 25),
                INSTANCE.parse("2020-09-18T01:02:03.4567890", 0, 23));
    }

    @Test
    public void testTrailingZ() {
        final String text = "2020-09-18T01:02:03.456";
        assertEquals(INSTANCE.parse(text), INSTANCE.parse(text + "Z"));
    }

    // Verify that timestamps without timezone are treated as local timestamps
    @Test
    public void datesWithNoTimezoneAreAssumedToBeLocal() {
        MilliTimestampLongConverter mtlc = new MilliTimestampLongConverter("America/New_York");
        assertEquals(mtlc.parse("2020-09-17T21:02:03.456-04:00"),
                mtlc.parse("2020-09-17T21:02:03.456"));
    }

    // Test if timestamps are correctly appended for Melbourne timezone
    @Test
    public void appendTest() {
        final MilliTimestampLongConverter converter = new MilliTimestampLongConverter("Australia/Melbourne");
        StringBuilder builder = new StringBuilder();
        converter.append(builder, TIMESTAMP);
        assertEquals(TIMESTAMP_STRING_MELBOURNE, builder.toString());
    }

    // Test if timestamps are correctly appended for UTC timezone
    @Test
    public void appendTestUTC() {
        final MilliTimestampLongConverter converter = new MilliTimestampLongConverter("UTC");
        StringBuilder builder = new StringBuilder();
        converter.append(builder, TIMESTAMP);
        assertEquals(TIMESTAMP_STRING_UTC_NO_SUFFIX, builder.toString());
    }

    // Test the round-trip conversion for various timezones, ensuring consistency in parsing and conversion back to string
    @Test
    public void roundTripTest() {
        MilliTimestampLongConverter utc = new MilliTimestampLongConverter("UTC");
        assertEquals(TIMESTAMP, utc.parse(TIMESTAMP_STRING_UTC_NO_SUFFIX), "roundTrip: utc parse");
        roundTrip(TIMESTAMP_STRING_UTC_NO_SUFFIX, TIMESTAMP, new MilliTimestampLongConverter("UTC"));
        roundTrip(TIMESTAMP_STRING_MELBOURNE, TIMESTAMP, new MilliTimestampLongConverter("Australia/Melbourne"));
    }

    // Helper method for round-trip tests: validates consistency in parsing and conversion
    private void roundTrip(String timestampString, long timestamp, LongConverter longConverter) {
        assertEquals(longConverter.asString(longConverter.parse(timestampString)), timestampString);
        assertEquals(longConverter.parse(longConverter.asString(timestamp)), timestamp);
    }
}
