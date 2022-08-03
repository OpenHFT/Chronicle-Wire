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

import net.openhft.chronicle.bytes.BytesMarshallable;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class DeserializeFromNakedFileTest extends WireTestCommon {
    @Test
    public void testPOJO() throws IOException {
        PlainOldJavaClass res = Marshallable.fromFile(PlainOldJavaClass.class, "naked.yaml");

        assertEquals(20, res.heartBtInt);
    }

    @Test
    public void testSelfDescribing() throws IOException {
        SelfDescribingClass res = Marshallable.fromFile(SelfDescribingClass.class, "naked.yaml");

        assertEquals(20, res.heartBtInt);
    }

    @Test
    public void testBytes() throws IOException {
        BytesClass res = Marshallable.fromFile(BytesClass.class, "naked.yaml");

        // The result of parsing first 4 bytes as integer value
        assertEquals(0x72616548, res.heartBtInt);
    }

    private static class PlainOldJavaClass {
        public int heartBtInt;
    }

    private static class SelfDescribingClass extends SelfDescribingMarshallable {
        public int heartBtInt;
    }

    private static class BytesClass implements BytesMarshallable {
        public int heartBtInt;
    }
}
