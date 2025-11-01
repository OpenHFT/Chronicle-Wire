/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

// This test class evaluates how different timestamp converters behave with timezone configurations.
public class TimestampLongConverterZonedIdsConfigTest extends WireTestCommon {

    // Cleanup the system properties after each test execution
    @After
    public void tearDown() {
        System.clearProperty(AbstractTimestampLongConverter.TIMESTAMP_LONG_CONVERTERS_ZONE_ID_SYSTEM_PROPERTY);
        System.clearProperty("mtlc.zoneId");
    }

    // Validate that the timezone for MilliTimestampLongConverter can be configured via a system property
    @Test
    public void timezoneCanBeConfiguredWithSystemPropertyForMilliseconds() {
        final long timestamp = MilliTimestampLongConverter.INSTANCE.parse("2020/09/18T01:02:03.123");
        System.setProperty(AbstractTimestampLongConverter.TIMESTAMP_LONG_CONVERTERS_ZONE_ID_SYSTEM_PROPERTY, "Australia/Melbourne");
        assertEquals("2020-09-18T11:02:03.123+10:00", new MilliTimestampLongConverter().asString(timestamp));
    }

    // Validate that the timezone for MicroTimestampLongConverter can be configured via a system property
    @Test
    public void timezoneCanBeConfiguredWithSystemPropertyForMicroseconds() {
        final long timestamp = MicroTimestampLongConverter.INSTANCE.parse("2020/09/18T01:02:03.123456");
        System.setProperty(AbstractTimestampLongConverter.TIMESTAMP_LONG_CONVERTERS_ZONE_ID_SYSTEM_PROPERTY, "America/Indiana/Indianapolis");
        assertEquals("2020-09-17T21:02:03.123456-04:00", new MicroTimestampLongConverter().asString(timestamp));
    }

    // Validate that the timezone for NanoTimestampLongConverter can be configured via a system property
    @Test
    public void timezoneCanBeConfiguredWithSystemPropertyForNanoseconds() {
        final long timestamp = NanoTimestampLongConverter.INSTANCE.parse("2020/09/18T01:02:03.123456789");
        System.setProperty(AbstractTimestampLongConverter.TIMESTAMP_LONG_CONVERTERS_ZONE_ID_SYSTEM_PROPERTY, "Asia/Ho_Chi_Minh");
        assertEquals("2020-09-18T08:02:03.123456789+07:00", new NanoTimestampLongConverter().asString(timestamp));
    }
}
