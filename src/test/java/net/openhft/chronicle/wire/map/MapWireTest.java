/*
 * Copyright 2016-2020 chronicle.software
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

package net.openhft.chronicle.wire.map;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class MapWireTest extends WireTestCommon {
    private final WireType wireType;
    @SuppressWarnings("rawtypes")
    private final Map m;

    @SuppressWarnings("rawtypes")
    public MapWireTest(WireType wireType, Map m) {
        this.wireType = wireType;
        this.m = m;
    }

    @NotNull
    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        @NotNull List<Object[]> list = new ArrayList<>();
        @NotNull WireType[] wireTypes = {WireType.TEXT, WireType.BINARY};
        for (WireType wt : wireTypes) {
            char maxValue = 256; // Character.MAX_VALUE;
            for (int i = 0; i < maxValue; i += 16) {
                @NotNull Map<Integer, String> map = new LinkedHashMap<>();
                for (int ch = i; ch < i + 16; ch++) {
                    if (Character.isValidCodePoint(ch)) {
                        final String s = Character.toString((char) ch);
                        map.put(i, s);
                    }
                }
                @NotNull Object[] test = {wt, map};
                list.add(test);
            }
        }
        return list;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void writeMap() {
        Bytes bytes = Bytes.elasticByteBuffer();
        Wire wire = wireType.apply(bytes);
        wire.getValueOut()
                .marshallable(m);
       // System.out.println(wire);

        @Nullable Map m2 = wire.getValueIn()
                .marshallableAsMap(Object.class, Object.class);
        assertEquals(m, m2);

        bytes.releaseLast();
    }
}
