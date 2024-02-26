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

import static net.openhft.chronicle.wire.MicroTimestampLongConverter.INSTANCE;
import static org.junit.Assert.assertEquals;

public class MicroTimestampLongConverterTest extends WireTestCommon {

    private static final String TIMESTAMP_STRING_UTC = "2023-02-15T05:31:49.856123Z";
    private static final String TIMESTAMP_STRING_UTC_NO_SUFFIX = "2023-02-15T05:31:49.856123";
    private static final long TIMESTAMP = 1676439109856123L;
    private static final String TIMESTAMP_STRING_MELBOURNE = "2023-02-15T16:31:49.856123+11:00";

    @Test
    public void parse() {
        long now = System.currentTimeMillis();
        // long parse1 = INSTANCE.parse(Long.toString(now));
        // assertEquals(now, parse1 / 1000);
        long parse2 = INSTANCE.parse(Long.toString(now * 1000));
        assertEquals(now, parse2 / 1000);
        long parse3 = INSTANCE.parse(INSTANCE.asString(now * 1000));
        assertEquals(now, parse3 / 1000);
    }

    @Test
    public void parse2() {
        assertEquals(INSTANCE.parse("2020/09/18T01:02:03.456789"),
                INSTANCE.parse("2020-09-18T01:02:03.456789"));
    }

    @Test
    public void parse3() {
        assertEquals(INSTANCE.parse("202020/09/18T01:02:03.456789", 2, 28),
                INSTANCE.parse("2020-09-18T01:02:03.4567890123", 0, 26));
    }

    @Test
    public void testTrailingZ() {
        final String text = "2020-09-18T01:02:03.456789";
        assertEquals(INSTANCE.parse(text), INSTANCE.parse(text + "Z"));
    }

    @Test
    public void NYparse() {
        MicroTimestampLongConverter mtlc = new MicroTimestampLongConverter("America/New_York");
        long time = INSTANCE.parse("2020/09/18T01:02:03.456789");
        final String str = mtlc.asString(time);
        assertEquals("2020-09-17T21:02:03.456789-04:00", str);
        assertEquals(time, mtlc.parse(str));
    }

    @Test
    public void datesWithNoTimezoneAreAssumedToBeLocal() {
        MicroTimestampLongConverter mtlc = new MicroTimestampLongConverter("America/New_York");
        assertEquals(mtlc.parse("2020-09-17T21:02:03.456789-04:00"),
                mtlc.parse("2020-09-17T21:02:03.456789"));
    }

    @Test
    public void appendTest() {
        final MicroTimestampLongConverter converter = new MicroTimestampLongConverter("Australia/Melbourne");
        StringBuilder builder = new StringBuilder();
        converter.append(builder, TIMESTAMP);
        assertEquals(TIMESTAMP_STRING_MELBOURNE, builder.toString());
    }

    @Test
    public void appendTestUTC() {
        final MicroTimestampLongConverter converter = new MicroTimestampLongConverter("UTC");
        StringBuilder builder = new StringBuilder();
        converter.append(builder, TIMESTAMP);
        assertEquals(TIMESTAMP_STRING_UTC_NO_SUFFIX, builder.toString());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void roundTripTest() {
        roundTrip(TIMESTAMP_STRING_UTC_NO_SUFFIX, TIMESTAMP, new MicroTimestampLongConverter("UTC"));
        roundTrip(TIMESTAMP_STRING_UTC, TIMESTAMP, new MicroTimestampLongConverter("UTC", true));
        roundTrip(TIMESTAMP_STRING_MELBOURNE, TIMESTAMP, new MicroTimestampLongConverter("Australia/Melbourne"));
    }

    private void roundTrip(String timestampString, long timestamp, LongConverter longConverter) {
        assertEquals(longConverter.asString(longConverter.parse(timestampString)), timestampString);
        assertEquals(longConverter.parse(longConverter.asString(timestamp)), timestamp);
    }
}
