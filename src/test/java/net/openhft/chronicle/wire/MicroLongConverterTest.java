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

import static org.junit.Assert.assertEquals;

/**
 * This class tests the functionality of the MicroLongConverter.
 * It extends the WireTestCommon for common test setup and utilities.
 */
public class MicroLongConverterTest extends WireTestCommon {

    /**
     * This test ensures that MicroLongConverter properly converts microsecond timestamp and duration representations.
     */
    @Test
    public void testMicro() {
        String in = "!net.openhft.chronicle.wire.MicroLongConverterTest$Data {\n" +
                "  time: 2019-01-20T23:45:11.123456,\n" +
                "  ttl: PT1H15M\n" +
                "}\n";
        // Deserialize the provided string into a Data object
        Data data = Marshallable.fromString(in);
        // Ensure the object's string representation matches the original input
        assertEquals(in, data.toString());
    }

    /**
     * Data class with fields representing time and duration, both using custom LongConverters.
     */
    static class Data extends SelfDescribingMarshallable {
        @LongConversion(MicroTimestampLongConverter.class)
        long time;  // Timestamp in microseconds

        @LongConversion(MicroDurationLongConverter.class)
        long ttl;   // Duration in microseconds
    }
}
