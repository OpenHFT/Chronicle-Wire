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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

@RunWith(value = Parameterized.class)
public class DeserializeFromNakedFileTest extends WireTestCommon {
    private final WireType wireType;

    public DeserializeFromNakedFileTest(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> combinations() {
        Object[][] list = {
                {WireType.TEXT},
                {WireType.YAML}
        };
        return Arrays.asList(list);
    }

    @Test
    public void testPOJO() throws IOException {
        PlainOldJavaClass res = wireType.fromFile(PlainOldJavaClass.class, "naked.yaml");

        assertEquals(20, res.heartBtInt);
    }

    @Test
    public void testSelfDescribing() throws IOException {
        SelfDescribingClass res = wireType.fromFile(SelfDescribingClass.class, "naked.yaml");

        assertEquals(20, res.heartBtInt);
    }

    @Test
    public void testBytes() throws IOException {
        assumeFalse(wireType == WireType.YAML);
        BytesClass res = wireType.fromFile(BytesClass.class, "naked.yaml");

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
