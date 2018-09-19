/*
 * Copyright 2016 higherfrequencytrading.com
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

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static net.openhft.chronicle.wire.WireType.TEXT;
import static org.junit.Assert.assertEquals;

/*
 * Created by Peter Lawrey on 17/03/16.
 */
public class ReadmePojoTest {
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(MyPojos.class);
    }

    @Test
    public void testFromString() throws IOException {
        @NotNull MyPojos mps = new MyPojos("test-list");
        mps.myPojos.add(new MyPojo("text1", 1, 1.1));
        mps.myPojos.add(new MyPojo("text2", 2, 2.2));

        System.out.println(mps);
        @Nullable MyPojos mps2 = Marshallable.fromString(mps.toString());
        assertEquals(mps, mps2);

        @NotNull String text = "!MyPojos {\n" +
                "  name: test-list,\n" +
                "  myPojos: [\n" +
                "    { text: text1, num: 1, factor: 1.1 },\n" +
                "    { text: text2, num: 2, factor: 2.2 }\n" +
                "  ]\n" +
                "}\n";
        @Nullable MyPojos mps3 = Marshallable.fromString(text);
        assertEquals(mps, mps3);

        @NotNull MyPojos mps4 = Marshallable.fromFile("my-pojos.yaml");
        assertEquals(mps, mps4);
    }

    @Test
    public void testMapDump() throws IOException {
        @NotNull Map<String, Object> map = new LinkedHashMap<>();
        map.put("text", "words");
        map.put("number", 1);
        map.put("factor", 1.1);
        map.put("list", Arrays.asList(1L, 2L, 3L, 4L));

        @NotNull Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("a", 1L);
        inner.put("b", "Hello World");
        inner.put("c", "bye");
        map.put("inner", inner);

        String text = TEXT.asString(map);
        assertEquals("text: words\n" +
                "number: !int 1\n" +
                "factor: 1.1\n" +
                "list: [\n" +
                "  1,\n" +
                "  2,\n" +
                "  3,\n" +
                "  4\n" +
                "],\n" +
                "inner: {\n" +
                "  a: 1,\n" +
                "  b: Hello World,\n" +
                "  c: bye\n" +
                "}\n", text);
        @Nullable Map<String, Object> map2 = TEXT.asMap(text);
        assertEquals(map, map2);
    }

    static class MyPojo extends AbstractMarshallable {
        String text;
        int num;
        double factor;

        public MyPojo(String text, int num, double factor) {
            this.text = text;
            this.num = num;
            this.factor = factor;
        }
    }

    static class MyPojos extends AbstractMarshallable {
        String name;
        @NotNull
        List<MyPojo> myPojos = new ArrayList<>();

        public MyPojos(String name) {
            this.name = name;
        }
    }
}
