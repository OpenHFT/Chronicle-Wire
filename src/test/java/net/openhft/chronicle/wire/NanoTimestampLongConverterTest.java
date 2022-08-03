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
    @Test
    public void parse() {
        long now = CLOCK.currentTimeNanos();
        long parse2 = INSTANCE.parse(Long.toString(now));
        assertEquals(now, parse2);
        String text = INSTANCE.asString(now);
        long parse3 = INSTANCE.parse(text);
        assertEquals(now, parse3);
    }

    @Test
    public void parse2() {
        assertEquals(INSTANCE.parse("2020/09/18T01:02:03.456789"),
                INSTANCE.parse("2020-09-18T01:02:03.456789"));
    }

    @Test
    public void datesWithNoTimezoneAreAssumedToBeLocal() {
        NanoTimestampLongConverter mtlc = new NanoTimestampLongConverter("America/New_York");
        assertEquals(mtlc.parse("2020-09-17T21:02:03.123456789-04:00"),
                mtlc.parse("2020-09-17T21:02:03.123456789"));
    }
}