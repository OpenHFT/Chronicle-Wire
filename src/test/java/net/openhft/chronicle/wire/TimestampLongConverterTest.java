/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.format.DateTimeParseException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A consolidated and parameterized test for all AbstractTimestampLongConverter implementations.
 * This class replaces the need for separate Milli, Micro, and Nano test classes,
 * reducing code duplication and improving maintainability.
 */
@SuppressWarnings({"unused", "deprecation", "removal"})
class TimestampLongConverterTest extends WireTestCommon {

    // Defines the data sets for each converter (Milli, Micro, Nano)
    private static Stream<Arguments> converters() {
        return Stream.of(
                Arguments.of(
                        "Milli",
                        (Function<String, LongConverter>) MilliTimestampLongConverter::new,
                        TimeUnit.MILLISECONDS,
                        1676439109856L,
                        "2023-02-15T05:31:49.856",
                        "2023-02-15T16:31:49.856+11:00"
                ),
                Arguments.of(
                        "Micro",
                        (Function<String, LongConverter>) MicroTimestampLongConverter::new,
                        TimeUnit.MICROSECONDS,
                        1676439109856123L,
                        "2023-02-15T05:31:49.856123",
                        "2023-02-15T16:31:49.856123+11:00"
                ),
                Arguments.of(
                        "Nano",
                        (Function<String, LongConverter>) NanoTimestampLongConverter::new,
                        TimeUnit.NANOSECONDS,
                        1676439109856123456L,
                        "2023-02-15T05:31:49.856123456",
                        "2023-02-15T16:31:49.856123456+11:00"
                )
        );
    }

    @DisplayName("Should parse a timestamp string with a 'Z' suffix")
    @ParameterizedTest(name = "{0}")
    @MethodSource("converters")
    void shouldParseUtcTimestampWithZ(String name, Function<String, LongConverter> factory, TimeUnit unit, long expectedTs, String utcTsString, String melbourneTsString) {
        LongConverter converter = factory.apply("UTC");
        assertEquals(expectedTs, converter.parse(utcTsString + "Z"),
                "UTC Z suffix should parse for converter " + name);
    }

    @DisplayName("Should assume local timezone for timestamps without an offset")
    @ParameterizedTest(name = "{0}")
    @MethodSource("converters")
    void shouldAssumeLocalZoneForDateWithNoTimezone(String name, Function<String, LongConverter> factory, TimeUnit unit, long expectedTs, String utcTsString, String melbourneTsString) {
        LongConverter melbourneConverter = factory.apply("Australia/Melbourne");
        // Parsing a string with an explicit offset should be the same as parsing a local time string without one
        assertEquals(melbourneConverter.parse(melbourneTsString), melbourneConverter.parse("2023-02-15T16:31:49.856123456"),
                "local zone parsing should match explicit offset for " + name);
    }

    @DisplayName("Should correctly format a timestamp to a non-UTC timezone")
    @ParameterizedTest(name = "{0}")
    @MethodSource("converters")
    void shouldFormatTimestampToLocalZone(String name, Function<String, LongConverter> factory, TimeUnit unit, long ts, String utcTsString, String melbourneTsString) {
        LongConverter converter = factory.apply("Australia/Melbourne");
        assertEquals(melbourneTsString, converter.asString(ts),
                "local zone formatting should match Melbourne for " + name);
    }

    @DisplayName("Should correctly format a timestamp to UTC (without 'Z' suffix)")
    @ParameterizedTest(name = "{0}")
    @MethodSource("converters")
    void shouldFormatTimestampToUtc(String name, Function<String, LongConverter> factory, TimeUnit unit, long ts, String utcTsString, String melbourneTsString) {
        LongConverter converter = factory.apply("UTC");
        assertEquals(utcTsString, converter.asString(ts),
                "UTC formatting should match expected string for " + name);
    }

    @DisplayName("Should perform a round trip from String -> long -> String")
    @ParameterizedTest(name = "{0}")
    @MethodSource("converters")
    void shouldRoundTripFromString(String name, Function<String, LongConverter> factory, TimeUnit unit, long ts, String utcTsString, String melbourneTsString) {
        LongConverter converter = factory.apply("Australia/Melbourne");
        long parsed = converter.parse(melbourneTsString);
        assertEquals(melbourneTsString, converter.asString(parsed),
                "string round-trip should preserve formatted value for " + name);
    }

    @DisplayName("Should perform a round trip from long -> String -> long")
    @ParameterizedTest(name = "{0}")
    @MethodSource("converters")
    void shouldRoundTripFromLong(String name, Function<String, LongConverter> factory, TimeUnit unit, long ts, String utcTsString, String melbourneTsString) {
        LongConverter converter = factory.apply("UTC");
        String asString = converter.asString(ts);
        assertEquals(ts, converter.parse(asString),
                "long round-trip should preserve value for " + name);
    }

    @DisplayName("Should parse date with slash separator")
    @ParameterizedTest(name = "{0}")
    @MethodSource("converters")
    void shouldParseDateWithSlashSeparator(String name, Function<String, LongConverter> factory, TimeUnit unit, long ts, String utcTsString, String melbourneTsString) {
        LongConverter converter = factory.apply("UTC");
        String slashDate = utcTsString.replaceFirst("-", "/").replaceFirst("-", "/");
        assertEquals(ts, converter.parse(slashDate),
                "slash date should parse to expected value for " + name);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Returns zero when parsing null or empty timestamp input string")
    @MethodSource("converters")
    void shouldReturnZeroForNullOrEmptyInput(String name, Function<String, LongConverter> factory, TimeUnit unit, long ts, String utcTsString, String melbourneTsString) {
        LongConverter converter = factory.apply("UTC");
        assertEquals(0, converter.parse(null),
                "null input should parse to zero for " + name);
        assertEquals(0, converter.parse(""),
                "empty input should parse to zero for " + name);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Parses numeric timestamp strings into epoch values")
    @MethodSource("converters")
    void shouldParseNumericTimestamp(String name, Function<String, LongConverter> factory, TimeUnit unit, long ts, String utcTsString, String melbourneTsString) {
        LongConverter converter = factory.apply("UTC");
        assertEquals(0L, converter.parse("0"),
                "numeric timestamp values should parse for " + name);
    }

    @DisplayName("Should format 0 and negative values as plain numbers")
    @ParameterizedTest(name = "{0}")
    @MethodSource("converters")
    void shouldFormatZeroAndNegativeAsNumber(String name, Function<String, LongConverter> factory, TimeUnit unit, long ts, String utcTsString, String melbourneTsString) {
        LongConverter converter = factory.apply("UTC");
        assertEquals("0", converter.asString(0),
                "zero value should format as 0 for " + name);
        assertEquals("-123", converter.asString(-123),
                "negative values should format as numbers for " + name);
    }

    @DisplayName("Should throw exception for malformed date strings")
    @ParameterizedTest(name = "{0}")
    @MethodSource("converters")
    void shouldThrowExceptionForMalformedDate(String name, Function<String, LongConverter> factory, TimeUnit unit, long ts, String utcTsString, String melbourneTsString) {
        LongConverter converter = factory.apply("UTC");
        assertThrows(DateTimeParseException.class, () -> converter.parse("not-a-valid-date"),
                "malformed date should throw for " + name);
        assertThrows(DateTimeParseException.class, () -> converter.parse("2023-13-40T00:00:00.000"),
                "invalid month or day should throw for " + name); // Invalid month/day
    }
}
