/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static net.openhft.chronicle.wire.MicroTimestampLongConverter.INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MicroTimestampLongConverterTest extends WireTestCommon {

    // Define constant strings for different timestamp representations
    private static final String TIMESTAMP_STRING_UTC = "2023-02-15T05:31:49.856123Z";
    private static final String TIMESTAMP_STRING_UTC_NO_SUFFIX = "2023-02-15T05:31:49.856123";
    private static final long TIMESTAMP = 1676439109856123L;
    private static final String TIMESTAMP_STRING_MELBOURNE = "2023-02-15T16:31:49.856123+11:00";

    // Test Cases
    // Test if the parse method correctly interprets timestamps
    @Test
    @DisplayName("Parse numeric micros and preserve millis")
    public void parse() {
        long now = System.currentTimeMillis();
        // Uncomment the below lines if needed
        // long parse1 = INSTANCE.parse(Long.toString(now));
        // assertEquals(now, parse1 / 1000);
        long parse2 = INSTANCE.parse(Long.toString(now * 1000));
        assertEquals(now, parse2 / 1000, "Parsing numeric micros should round-trip millis");
        long parse3 = INSTANCE.parse(INSTANCE.asString(now * 1000));
        assertEquals(now, parse3 / 1000, "String conversion should round-trip micros");
    }

    // Test different date format parsing
    @Test
    @DisplayName("Parse slash and dash formats consistently")
    public void parse2() {
        assertEquals(INSTANCE.parse("2020/09/18T01:02:03.456"),
                INSTANCE.parse("2020-09-18T01:02:03.456"),
                "Slash and dash formats should parse identically for millis");
        assertEquals(INSTANCE.parse("2020/09/18T01:02:03.456789"),
                INSTANCE.parse("2020-09-18T01:02:03.456789"),
                "Slash and dash formats should parse identically for micros");
        assertEquals(INSTANCE.parse("2020/09/18T01:02:03.456789"),
                INSTANCE.parse("2020-09-18T01:02:03.456789012"),
                "Extra nanos should not change micro parsing");
    }

    // Check if trailing 'Z' in timestamp does not affect parsing
    @Test
    @DisplayName("Parse subsequences and ignore extra digits")
    public void parse3() {
        assertEquals(INSTANCE.parse("202020/09/18T01:02:03.456789", 2, 28),
                INSTANCE.parse("2020-09-18T01:02:03.4567890123", 0, 26),
                "Subsequence parsing should match full-string parsing");
    }

    @Test
    @DisplayName("Ignore trailing Z when parsing micros")
    public void testTrailingZ() {
        final String text = "2020-09-18T01:02:03.456789";
        assertEquals(INSTANCE.parse(text), INSTANCE.parse(text + "Z"),
                "Trailing Z should not affect micro parsing");
    }

    // Test timestamp parsing with New York timezone
    @Test
    @DisplayName("Parse and render timestamps in New York")
    public void parseNewYorkTimeZone() {
        MicroTimestampLongConverter mtlc = new MicroTimestampLongConverter("America/New_York");
        long time = INSTANCE.parse("2020/09/18T01:02:03.456789");
        final String str = mtlc.asString(time);
        assertEquals("2020-09-17T21:02:03.456789-04:00", str,
                "New York timezone should adjust output timestamp");
        assertEquals(time, mtlc.parse(str),
                "Parsing New York timestamp should return original micros");
    }

    // Verify that timestamps without timezone are treated as local timestamps
    @Test
    @DisplayName("Assume local zone when timestamp lacks offset")
    public void datesWithNoTimezoneAreAssumedToBeLocal() {
        MicroTimestampLongConverter mtlc = new MicroTimestampLongConverter("America/New_York");
        assertEquals(mtlc.parse("2020-09-17T21:02:03.456789-04:00"),
                mtlc.parse("2020-09-17T21:02:03.456789"),
                "Timestamp without timezone should be treated as local");
    }

    // Test if timestamps are correctly appended for Melbourne timezone
    @Test
    @DisplayName("Append Melbourne timestamp with zone offset")
    public void appendTest() {
        final MicroTimestampLongConverter converter = new MicroTimestampLongConverter("Australia/Melbourne");
        StringBuilder builder = new StringBuilder();
        converter.append(builder, TIMESTAMP);
        assertEquals(TIMESTAMP_STRING_MELBOURNE, builder.toString(),
                "Append should render Melbourne timezone timestamp");
    }

    // Test if timestamps are correctly appended for UTC timezone
    @Test
    @DisplayName("Append UTC timestamp without zone suffix")
    public void appendTestUTC() {
        final MicroTimestampLongConverter converter = new MicroTimestampLongConverter("UTC");
        StringBuilder builder = new StringBuilder();
        converter.append(builder, TIMESTAMP);
        assertEquals(TIMESTAMP_STRING_UTC_NO_SUFFIX, builder.toString(),
                "Append should render UTC timestamp without suffix");
    }

    // Test round-trip conversion for various timezones
    @Test
    @DisplayName("Round-trip micros for UTC and Melbourne")
    public void roundTripTest() {
        MicroTimestampLongConverter utc = new MicroTimestampLongConverter("UTC");
        assertEquals(TIMESTAMP, utc.parse(TIMESTAMP_STRING_UTC_NO_SUFFIX),
                "UTC parse should round-trip micros value");
        roundTrip(TIMESTAMP_STRING_UTC_NO_SUFFIX, TIMESTAMP, new MicroTimestampLongConverter("UTC"));
        roundTrip(TIMESTAMP_STRING_MELBOURNE, TIMESTAMP, new MicroTimestampLongConverter("Australia/Melbourne"));
    }

    // Helper method for round-trip tests
    private void roundTrip(String timestampString, long timestamp, LongConverter longConverter) {
        assertEquals(longConverter.asString(longConverter.parse(timestampString)), timestampString,
                "String conversion should round-trip timestamp");
        assertEquals(longConverter.parse(longConverter.asString(timestamp)), timestamp,
                "Long conversion should round-trip timestamp");
    }
}
