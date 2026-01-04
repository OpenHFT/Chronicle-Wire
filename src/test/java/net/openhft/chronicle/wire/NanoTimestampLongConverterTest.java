/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static net.openhft.chronicle.core.time.SystemTimeProvider.CLOCK;
import static net.openhft.chronicle.wire.NanoTimestampLongConverter.INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NanoTimestampLongConverterTest extends WireTestCommon {

    // Static constants for test cases.
    private static final String TIMESTAMP_STRING_UTC = "2023-02-15T05:31:49.856123456Z";
    private static final String TIMESTAMP_STRING_UTC_NO_SUFFIX = "2023-02-15T05:31:49.856123456";
    private static final long TIMESTAMP = 1676439109856123456L;
    private static final String TIMESTAMP_STRING_MELBOURNE = "2023-02-15T16:31:49.856123456+11:00";

    // Testing parsing of nanosecond timestamps.
    @Test
    @DisplayName("Parse numeric nanos and preserve value")
    public void parse() {
        // Getting current nanosecond timestamp.
        long now = CLOCK.currentTimeNanos();

        // Testing conversion of the nanosecond timestamp to string and back to long.
        long parse2 = INSTANCE.parse(Long.toString(now));
        assertEquals(now, parse2, "Parsing numeric nanos should round-trip the value");

        // Testing conversion from long to string representation of timestamp and then parsing it back to long.
        String text = INSTANCE.asString(now);
        long parse3 = INSTANCE.parse(text);
        assertEquals(now, parse3, "String conversion should round-trip nanos");
    }

    // Testing parsing of different string formats for nanosecond timestamps.
    @Test
    @DisplayName("Parse slash and dash timestamp formats")
    public void parseString() {
        assertEquals(INSTANCE.parse("2020/09/18T01:02:03.456"),
                INSTANCE.parse("2020-09-18T01:02:03.456"),
                "Slash and dash formats should parse identically for millis");
        assertEquals(INSTANCE.parse("2020/09/18T01:02:03.456789"),
                INSTANCE.parse("2020-09-18T01:02:03.456789"),
                "Slash and dash formats should parse identically for micros");
        assertEquals(INSTANCE.parse("2020/09/18T01:02:03.456789012"),
                INSTANCE.parse("2020-09-18T01:02:03.456789012"),
                "Slash and dash formats should parse identically for nanos");
    }

    @Test
    @DisplayName("Parse subsequences and ignore extra digits")
    public void parseSubsequence() {
        assertEquals(INSTANCE.parse("202020/09/18T01:02:03.456789012", 2, 31),
                INSTANCE.parse("2020-09-18T01:02:03.4567890123", 0, 29),
                "Subsequence parsing should match full-string parsing");
    }

    // Testing assumption of default timezone (local) if no timezone is provided.
    @Test
    @DisplayName("Assume local zone when timestamp lacks offset")
    public void datesWithNoTimezoneAreAssumedToBeLocal() {
        NanoTimestampLongConverter mtlc = new NanoTimestampLongConverter("America/New_York");
        assertEquals(mtlc.parse("2020-09-17T21:02:03.123456789-04:00"),
                mtlc.parse("2020-09-17T21:02:03.123456789"),
                "Timestamp without timezone should be treated as local");
    }

    // Testing conversion of nanosecond timestamp to Melbourne timezone.
    @Test
    @DisplayName("Append Melbourne timestamp with zone offset")
    public void appendTest() {
        final NanoTimestampLongConverter converter = new NanoTimestampLongConverter("Australia/Melbourne");
        StringBuilder builder = new StringBuilder();
        converter.append(builder, TIMESTAMP);
        assertEquals(TIMESTAMP_STRING_MELBOURNE, builder.toString(),
                "Append should render Melbourne timezone timestamp");
    }

    // Testing conversion of nanosecond timestamp to UTC.
    @Test
    @DisplayName("Append UTC timestamp without zone suffix")
    public void appendTestUTC() {
        final NanoTimestampLongConverter converter = new NanoTimestampLongConverter("UTC");
        StringBuilder builder = new StringBuilder();
        converter.append(builder, TIMESTAMP);
        assertEquals(TIMESTAMP_STRING_UTC_NO_SUFFIX, builder.toString(),
                "Append should render UTC timestamp without suffix");
    }

    // Testing round-trip conversions (from string to long and back) for different timezones and formats.
    @Test
    @DisplayName("Round-trip nanos for UTC and Melbourne")
    public void roundTripTest() {
        NanoTimestampLongConverter utc = new NanoTimestampLongConverter("UTC");
        assertEquals(TIMESTAMP, utc.parse(TIMESTAMP_STRING_UTC_NO_SUFFIX),
                "UTC parse should round-trip nanos value");
        roundTrip(TIMESTAMP_STRING_MELBOURNE, TIMESTAMP, new NanoTimestampLongConverter("Australia/Melbourne"));
        roundTrip(TIMESTAMP_STRING_UTC_NO_SUFFIX, TIMESTAMP, new NanoTimestampLongConverter("UTC"));
    }

    // Helper method for the round-trip conversions.
    private void roundTrip(String timestampString, long timestamp, LongConverter longConverter) {
        assertEquals(timestamp, longConverter.parse(longConverter.asString(timestamp)),
                "Long conversion should round-trip timestamp");
        assertEquals(timestampString, longConverter.asString(longConverter.parse(timestampString)),
                "String conversion should round-trip timestamp");
    }
}
