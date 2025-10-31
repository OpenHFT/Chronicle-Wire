/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Exercises number formats and booleans across wire types.
 */
public class WireNumberFormatsTest extends WireTestCommon {

    @Test
    public void textFormats() {
        String s = "i: +5\n" +
                "j: 00\n" +
                "d: 1e3\n" +
                "b: true\n" +
                "mz: -0.0\n";
        TextWire w = TextWire.from(s);
        assertEquals(5, w.read("i").int32());
        assertEquals(0, w.read("j").int32());
        assertEquals(1000.0, w.read("d").float64(), 0.0);
        assertTrue(w.read("b").bool());
        assertEquals(0.0, w.read("mz").float64(), 0.0);
    }

    @Test
    public void yamlFormats() {
        String s = "i: +7\n" +
                "j: 000\n" +
                "d: 2.5e2\n" +
                "b: false\n" +
                "mz: -0.0\n";
        YamlWire w = YamlWire.from(s);
        assertEquals(7, w.read("i").int32());
        assertEquals(0, w.read("j").int32());
        assertEquals(250.0, w.read("d").float64(), 0.0);
        assertFalse(w.read("b").bool());
        assertEquals(0.0, w.read("mz").float64(), 0.0);
    }

    @Test
    public void binaryExtremesRoundTrip() {
        Wire w = WireType.BINARY.apply(Bytes.allocateElasticOnHeap(256));
        w.write("i32min").int32(Integer.MIN_VALUE);
        w.write("i32max").int32(Integer.MAX_VALUE);
        w.write("i64min").int64(Long.MIN_VALUE + 1);
        w.write("i64max").int64(Long.MAX_VALUE);
        w.write("pi").float64(Math.PI);
        w.write("mz").float64(-0.0);

        assertEquals(Integer.MIN_VALUE, w.read("i32min").int32());
        assertEquals(Integer.MAX_VALUE, w.read("i32max").int32());
        assertEquals(Long.MIN_VALUE + 1, w.read("i64min").int64());
        assertEquals(Long.MAX_VALUE, w.read("i64max").int64());
        assertEquals(Math.PI, w.read("pi").float64(), 0.0);
        assertEquals(0.0, w.read("mz").float64(), 0.0);
    }
}

