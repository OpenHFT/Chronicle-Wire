/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static net.openhft.chronicle.wire.MilliTimestampLongConverter.INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"deprecation", "removal"})
class MilliTimestampLongConverterTest extends WireTestCommon {

    // Define constant strings for different timestamp representations
    private static final String TIMESTAMP_STRING_UTC = "2023-02-15T05:31:49.856Z";
    private static final String TIMESTAMP_STRING_UTC_NO_SUFFIX = "2023-02-15T05:31:49.856";
    private static final long TIMESTAMP = 1676439109856L;
    private static final String TIMESTAMP_STRING_MELBOURNE = "2023-02-15T16:31:49.856+11:00";

    // Test Cases
    // Test the parsing functionality of the converter with current time
    @Test
    @DisplayName("Parse numeric millis and preserve value")
    void parse() {
        long now = System.currentTimeMillis();

        // Parse the current timestamp from its string representation and verify equality
        long parse1 = INSTANCE.parse(Long.toString(now));
        assertEquals(now, parse1, "Parsing numeric millis should round-trip");
        long parse2 = INSTANCE.parse(Long.toString(now));
        assertEquals(now, parse2, "Repeated parse of numeric millis should round-trip");
        String text = INSTANCE.asString(now);
        long parse3 = INSTANCE.parse(text);
        assertEquals(now, parse3, "String conversion should round-trip millis");
    }

    // Test different date format parsing
    @Test
    @DisplayName("Parse slash and dash formats consistently")
    void parse2() {
        assertEquals(INSTANCE.parse("2020/09/18T01:02:03.456"),
                INSTANCE.parse("2020-09-18T01:02:03.456"),
                "Slash and dash formats should parse identically for millis");
        assertEquals(INSTANCE.parse("2020/09/18T01:02:03.456"),
                INSTANCE.parse("2020-09-18T01:02:03.456789"),
                "Sub-millis precision should not change millis parsing");
        assertEquals(INSTANCE.parse("2020/09/18T01:02:03.456"),
                INSTANCE.parse("2020-09-18T01:02:03.456789012"),
                "Nanos precision should not change millis parsing");
    }

    // Test if trailing 'Z' in the timestamp does not affect parsing
    @Test
    @DisplayName("Parse subsequences and ignore extra digits")
    void parse3() {
        assertEquals(INSTANCE.parse("202020/09/18T01:02:03.456", 2, 25),
                INSTANCE.parse("2020-09-18T01:02:03.4567890", 0, 23),
                "Subsequence parsing should match full-string parsing");
    }

    @Test
    @DisplayName("Ignore trailing Z when parsing millis")
    void testTrailingZ() {
        final String text = "2020-09-18T01:02:03.456";
        assertEquals(INSTANCE.parse(text), INSTANCE.parse(text + "Z"),
                "Trailing Z should not affect millis parsing");
    }

    // Verify that timestamps without timezone are treated as local timestamps
    @Test
    @DisplayName("Assume local zone when timestamp lacks offset")
    void datesWithNoTimezoneAreAssumedToBeLocal() {
        MilliTimestampLongConverter mtlc = new MilliTimestampLongConverter("America/New_York");
        assertEquals(mtlc.parse("2020-09-17T21:02:03.456-04:00"),
                mtlc.parse("2020-09-17T21:02:03.456"),
                "Timestamp without timezone should be treated as local");
    }

    // Test if timestamps are correctly appended for Melbourne timezone
    @Test
    @DisplayName("Append Melbourne timestamp with zone offset")
    void appendTest() {
        final MilliTimestampLongConverter converter = new MilliTimestampLongConverter("Australia/Melbourne");
        StringBuilder builder = new StringBuilder();
        converter.append(builder, TIMESTAMP);
        assertEquals(TIMESTAMP_STRING_MELBOURNE, builder.toString(),
                "Append should render Melbourne timezone timestamp");
    }

    // Test if timestamps are correctly appended for UTC timezone
    @Test
    @DisplayName("Append UTC timestamp without zone suffix")
    void appendTestUTC() {
        final MilliTimestampLongConverter converter = new MilliTimestampLongConverter("UTC");
        StringBuilder builder = new StringBuilder();
        converter.append(builder, TIMESTAMP);
        assertEquals(TIMESTAMP_STRING_UTC_NO_SUFFIX, builder.toString(),
                "Append should render UTC timestamp without suffix");
    }

    // Test the round-trip conversion for various timezones, ensuring consistency in parsing and conversion back to string
    @Test
    @DisplayName("Round-trip millis for UTC and Melbourne")
    void roundTripTest() {
        MilliTimestampLongConverter utc = new MilliTimestampLongConverter("UTC");
        assertEquals(TIMESTAMP, utc.parse(TIMESTAMP_STRING_UTC_NO_SUFFIX),
                "UTC parse should round-trip millis value");
        roundTrip(TIMESTAMP_STRING_UTC_NO_SUFFIX, TIMESTAMP, new MilliTimestampLongConverter("UTC"));
        roundTrip(TIMESTAMP_STRING_MELBOURNE, TIMESTAMP, new MilliTimestampLongConverter("Australia/Melbourne"));
    }

    // Helper method for round-trip tests: validates consistency in parsing and conversion
    private void roundTrip(String timestampString, long timestamp, LongConverter longConverter) {
        assertEquals(longConverter.asString(longConverter.parse(timestampString)), timestampString,
                "String conversion should round-trip timestamp");
        assertEquals(longConverter.parse(longConverter.asString(timestamp)), timestamp,
                "Long conversion should round-trip timestamp");
    }
}
