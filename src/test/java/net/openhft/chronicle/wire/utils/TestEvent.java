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

package net.openhft.chronicle.wire.utils;

import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.NanoTimestampLongConverter;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;

/**
 * Represents a test event with specific time attributes. It extends the SelfDescribingMarshallable class
 * to provide serialization and deserialization capabilities.
 */
public class TestEvent extends SelfDescribingMarshallable {

    // The time at which the event occurred, represented as a long. The annotation @LongConversion
    // specifies that this long value should be converted using the NanoTimestampLongConverter class,
    // which likely converts between a long value and a more human-readable time format.
    @LongConversion(NanoTimestampLongConverter.class)
    long eventTime;

    // The time at which the event was processed. Similar to eventTime, it's also a long value
    // converted using the NanoTimestampLongConverter.
    @LongConversion(NanoTimestampLongConverter.class)
    long processedTime;

    // Represents the current time when the event is being handled, in a similar format to
    // eventTime and processedTime.
    @LongConversion(NanoTimestampLongConverter.class)
    long currentTime;
}
