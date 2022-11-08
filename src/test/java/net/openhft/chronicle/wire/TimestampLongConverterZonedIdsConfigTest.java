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

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimestampLongConverterZonedIdsConfigTest extends WireTestCommon {

    @After
    public void tearDown() {
        System.clearProperty(AbstractTimestampLongConverter.TIMESTAMP_LONG_CONVERTERS_ZONE_ID_SYSTEM_PROPERTY);
        System.clearProperty("mtlc.zoneId");
    }

    @Test
    public void timezoneCanBeConfiguredWithSystemPropertyForMilliseconds() {
        final long timestamp = MilliTimestampLongConverter.INSTANCE.parse("2020/09/18T01:02:03.123");
        System.setProperty(AbstractTimestampLongConverter.TIMESTAMP_LONG_CONVERTERS_ZONE_ID_SYSTEM_PROPERTY, "Australia/Melbourne");
        assertEquals("2020-09-18T11:02:03.123+10:00", new MilliTimestampLongConverter().asString(timestamp));
    }

    @Test
    public void timezoneCanBeConfiguredWithSystemPropertyForMicroseconds() {
        final long timestamp = MicroTimestampLongConverter.INSTANCE.parse("2020/09/18T01:02:03.123456");
        System.setProperty(AbstractTimestampLongConverter.TIMESTAMP_LONG_CONVERTERS_ZONE_ID_SYSTEM_PROPERTY, "America/Indiana/Indianapolis");
        assertEquals("2020-09-17T21:02:03.123456-04:00", new MicroTimestampLongConverter().asString(timestamp));
    }

    @Test
    public void timezoneCanBeConfiguredWithLegacySystemPropertyForMicroseconds() {
        final long timestamp = MicroTimestampLongConverter.INSTANCE.parse("2020/09/18T01:02:03.123456");
        System.setProperty("mtlc.zoneId", "Europe/Paris");
        expectException("mtlc.zoneId has been deprecated");
        assertEquals("2020-09-18T03:02:03.123456+02:00", new MicroTimestampLongConverter().asString(timestamp));
    }

    @Test
    public void timezoneCanBeConfiguredWithSystemPropertyForNanoseconds() {
        final long timestamp = NanoTimestampLongConverter.INSTANCE.parse("2020/09/18T01:02:03.123456789");
        System.setProperty(AbstractTimestampLongConverter.TIMESTAMP_LONG_CONVERTERS_ZONE_ID_SYSTEM_PROPERTY, "Asia/Ho_Chi_Minh");
        assertEquals("2020-09-18T08:02:03.123456789+07:00", new NanoTimestampLongConverter().asString(timestamp));
    }
}
