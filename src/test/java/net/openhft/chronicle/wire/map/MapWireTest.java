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

package net.openhft.chronicle.wire.map;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

import static org.junit.Assert.assertEquals;

/*
 * Created by Peter Lawrey on 09/05/16.
 */
@RunWith(value = Parameterized.class)
public class MapWireTest {
    private final WireType wireType;
    private final Map m;

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
            for (int i = 0; i < Character.MAX_VALUE; i += 128) {
                @NotNull Map<Integer, String> map = new LinkedHashMap<>();
                for (int ch = i; ch < i + 128; ch++) {
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

    @Test
    public void writeMap() {
        Bytes bytes = Bytes.elasticByteBuffer();
        Wire wire = wireType.apply(bytes);
        wire.getValueOut()
                .marshallable(m);
//        System.out.println(wire);

        @Nullable Map m2 = wire.getValueIn()
                .marshallableAsMap(Object.class, Object.class);
        assertEquals(m, m2);

        bytes.release();
    }

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }
}
