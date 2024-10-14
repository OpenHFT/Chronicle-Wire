/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import org.junit.Test;

import static net.openhft.chronicle.core.time.SystemTimeProvider.CLOCK;
import static net.openhft.chronicle.wire.NanoTimestampLongConverter.INSTANCE;
import static org.junit.Assert.assertEquals;

public class NanoTimestampLongConverterTest extends WireTestCommon {

    // Static constants for test cases.
    private static final String TIMESTAMP_STRING_UTC = "2023-02-15T05:31:49.856123456Z";
    private static final String TIMESTAMP_STRING_UTC_NO_SUFFIX = "2023-02-15T05:31:49.856123456";
    private static final long TIMESTAMP = 1676439109856123456L;
    private static final String TIMESTAMP_STRING_MELBOURNE = "2023-02-15T16:31:49.856123456+11:00";

    // Testing parsing of nanosecond timestamps.
    @Test
    public void parse() {
        // Getting current nanosecond timestamp.
        long now = CLOCK.currentTimeNanos();

        // Testing conversion of the nanosecond timestamp to string and back to long.
        long parse2 = INSTANCE.parse(Long.toString(now));
        assertEquals(now, parse2);

        // Testing conversion from long to string representation of timestamp and then parsing it back to long.
        String text = INSTANCE.asString(now);
        long parse3 = INSTANCE.parse(text);
        assertEquals(now, parse3);
    }

    // Testing parsing of different string formats for nanosecond timestamps.
    @Test
    public void parseString() {
        assertEquals(INSTANCE.parse("2020/09/18T01:02:03.456789012"),
                INSTANCE.parse("2020-09-18T01:02:03.456789012"));
    }

    @Test
    public void parseSubsequence() {
        assertEquals(INSTANCE.parse("202020/09/18T01:02:03.456789012", 2, 31),
                INSTANCE.parse("2020-09-18T01:02:03.4567890123", 0, 29));
    }

    // Testing assumption of default timezone (local) if no timezone is provided.
    @Test
    public void datesWithNoTimezoneAreAssumedToBeLocal() {
        NanoTimestampLongConverter mtlc = new NanoTimestampLongConverter("America/New_York");
        assertEquals(mtlc.parse("2020-09-17T21:02:03.123456789-04:00"),
                mtlc.parse("2020-09-17T21:02:03.123456789"));
    }

    // Testing conversion of nanosecond timestamp to Melbourne timezone.
    @Test
    public void appendTest() {
        final NanoTimestampLongConverter converter = new NanoTimestampLongConverter("Australia/Melbourne");
        StringBuilder builder = new StringBuilder();
        converter.append(builder, TIMESTAMP);
        assertEquals(TIMESTAMP_STRING_MELBOURNE, builder.toString());
    }

    // Testing conversion of nanosecond timestamp to UTC.
    @Test
    public void appendTestUTC() {
        final NanoTimestampLongConverter converter = new NanoTimestampLongConverter("UTC");
        StringBuilder builder = new StringBuilder();
        converter.append(builder, TIMESTAMP);
        assertEquals(TIMESTAMP_STRING_UTC_NO_SUFFIX, builder.toString());
    }

    // Testing round-trip conversions (from string to long and back) for different timezones and formats.
    @Test
    public void roundTripTest() {
        roundTrip(TIMESTAMP_STRING_MELBOURNE, TIMESTAMP, new NanoTimestampLongConverter("Australia/Melbourne"));
        roundTrip(TIMESTAMP_STRING_UTC_NO_SUFFIX, TIMESTAMP, new NanoTimestampLongConverter("UTC"));
    }

    // Helper method for the round-trip conversions.
    private void roundTrip(String timestampString, long timestamp, LongConverter longConverter) {
        assertEquals(timestamp, longConverter.parse(longConverter.asString(timestamp)));
        assertEquals(timestampString, longConverter.asString(longConverter.parse(timestampString)));
    }
}
