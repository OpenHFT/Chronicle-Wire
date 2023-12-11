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

package net.openhft.chronicle.wire.map;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.Wires;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

public class MapMarshallableTest extends WireTestCommon {

    @Test
    public void test() {
        @NotNull final Map<String, Object> map = new LinkedHashMap<>();
        map.put("one", 10);
        map.put("two", 20);
        map.put("three", 30);

        @NotNull MyDto usingInstance = new MyDto();
        @NotNull MyDto result = Wires.copyTo(map, usingInstance);
        assertEquals(10, result.one);
        assertEquals(20, result.two);
        assertEquals(30, result.three);

        @NotNull Map<String, Object> map2 = Wires.copyTo(result, new LinkedHashMap<>());
        assertEquals("{one=10, two=20, three=30}", map2.toString());

        @NotNull Map<String, Object> map3 = Wires.copyTo(map, new TreeMap<>());
        assertEquals("{one=10, three=30, two=20}", map3.toString());
    }

    private static class MyDto extends SelfDescribingMarshallable {
        int one;
        int two;
        int three;
    }
}
