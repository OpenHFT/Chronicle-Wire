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

import static net.openhft.chronicle.wire.NanoTimestampLongConverter.INSTANCE;
import static org.junit.Assert.assertEquals;

public class NanoLongConverterTest extends WireTestCommon {
    @Test
    public void testNano() {
        String in = "!net.openhft.chronicle.wire.NanoLongConverterTest$Data {\n" +
                "  time: 2019-01-20T23:45:11.123456789,\n" +
                "  ttl: PT1H15M\n" +
                "}\n";
        Data data = Marshallable.fromString(in);
        assertEquals(in, data.toString());
    }

    @Test
    public void testTrailingZ() {
        final String text = "2019-01-20T23:45:11.123456789";
        assertEquals(INSTANCE.parse(text), INSTANCE.parse(text + "Z"));
    }

    static class Data extends SelfDescribingMarshallable {
        @LongConversion(NanoTimestampLongConverter.class)
        long time;
        @LongConversion(NanoDurationLongConverter.class)
        long ttl;
    }
}
