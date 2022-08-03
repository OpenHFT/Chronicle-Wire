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
}