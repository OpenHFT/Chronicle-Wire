/*
 * Copyright 2016 higherfrequencytrading.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

/*
 * Created by Peter Lawrey on 10/06/16.
 */
@RunWith(value = Parameterized.class)
public class PrimArraysTest {

    private final WireType wireType;
    private final Object array;
    private final String asText;

    public PrimArraysTest(WireType wireType, Object array, String asText) {
        this.wireType = wireType;
        this.array = array;
        this.asText = asText;
    }

    @NotNull
    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        @NotNull List<Object[]> list = new ArrayList<>();
        for (WireType wt : new WireType[]{
                WireType.TEXT,
                WireType.BINARY
        }) {
            @NotNull final Object[] objects = {
                    new boolean[]{true, false},
                    "test: !boolean[] [ true, false ]",
                    "test: !boolean[] [ ]",
                    new byte[]{Byte.MIN_VALUE, 0, Byte.MAX_VALUE},
                    "test: !byte[] !!binary gAB/\n",
                    "test: !byte[] !!binary \n",
                    new char[]{Character.MIN_VALUE, '?', Character.MAX_VALUE},
                    "test: !char[] [ \"\\0\", \"?\", \"\\uFFFF\" ]",
                    "test: !char[] [ ]",
                    new short[]{Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE},
                    "test: !short[] [ -32768, -1, 0, 1, 32767 ]",
                    "test: !short[] [ ]",
                    new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE},
                    "test: !int[] [ -2147483648, -1, 0, 1, 2147483647 ]",
                    "test: !int[] [ ]",
                    new long[]{Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE},
                    "test: !long[] [ -9223372036854775808, -1, 0, 1, 9223372036854775807 ]",
                    "test: !long[] [ ]",
                    new float[]{Float.MIN_VALUE, -1, 0, 1, Float.MAX_VALUE},
                    "test: !float[] [ 1.4E-45, -1.0, 0.0, 1.0, 3.4028235E38 ]",
                    "test: !float[] [ ]",
                    new double[]{Double.MIN_VALUE, -1, 0, 1, Double.MAX_VALUE},
                    "test: !double[] [ 4.9E-324, -1.0, 0.0, 1.0, 1.7976931348623157E308 ]",
                    "test: !double[] [ ]"
            };
            for (int i = 0; i < objects.length; i += 3) {
                Object array = objects[i];
                list.add(new Object[]{wt, array, objects[i + 1]});
                final Object emptyArray = Array.newInstance(array.getClass().getComponentType(), 0);
                list.add(new Object[]{wt, emptyArray, objects[i + 2]});
            }
        }
        return list;
    }

    @Test
    public void testPrimArray() {
        Wire wire = createWire();
        try {
            wire.write("test")
                    .object(array);
            System.out.println(wire);
            if (wireType == WireType.TEXT)
                assertEquals(asText, wire.toString());

            @Nullable Object array2 = wire.read().object();
            assertEquals(array.getClass(), array2.getClass());
            assertEquals(Array.getLength(array), Array.getLength(array));
            for (int i = 0, len = Array.getLength(array); i < len; i++)
                assertEquals(Array.get(array, i), Array.get(array2, i));
        } finally {
            wire.bytes().release();
        }
    }

    private Wire createWire() {
        return wireType.apply(Bytes.elasticByteBuffer());
    }

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }
}
