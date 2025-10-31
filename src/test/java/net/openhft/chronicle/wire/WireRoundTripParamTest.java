/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Basic round‑trip checks across a few {@link WireType}s.
 * Covers primitive scalars and a small sequence to exercise ValueIn/ValueOut
 * for Binary/Text/YAML wires.
 */
public class WireRoundTripParamTest extends WireTestCommon {

    private static final WireType[] TYPES = new WireType[]{
            WireType.BINARY,
            WireType.TEXT,
            WireType.YAML
    };

    @Test
    public void primitivesRoundTrip() {
        for (WireType wt : TYPES) {
            Wire w = wt.apply(Bytes.allocateElasticOnHeap(256));

            w.write("i8").int8((byte) -1);
            w.write("i16").int16((short) 32767);
            w.write("i32").int32(123456789);
            w.write("i64").int64(Long.MIN_VALUE + 1);
            w.write("fp").float32(3.25f);
            w.write("dp").float64(Math.PI);
            w.write("b").bool(true);
            w.write("txt").text("hello");

            // read back
            assertEquals((byte) -1, w.read("i8").int8());
            assertEquals(32767, w.read("i16").int16());
            assertEquals(123456789, w.read("i32").int32());
            assertEquals(Long.MIN_VALUE + 1, w.read("i64").int64());
            assertEquals(3.25f, w.read("fp").float32(), 0.0f);
            assertEquals(Math.PI, w.read("dp").float64(), 0.0);
            assertTrue(w.read("b").bool());
            assertEquals("hello", w.read("txt").text());
        }
    }

    @Test
    public void sequenceRoundTrip() {
        for (WireType wt : TYPES) {
            Wire w = wt.apply(Bytes.allocateElasticOnHeap(256));

            w.write("seq").sequence(v -> {
                v.int32(1);
                v.text("two");
                v.int64(3L);
            });

            final Object[] out = new Object[3];
            w.read("seq").sequence(out, (arr, in) -> {
                arr[0] = in.int32();
                arr[1] = in.text();
                arr[2] = in.int64();
            });

            assertArrayEquals(new Object[]{1, "two", 3L}, out);
        }
    }
}
