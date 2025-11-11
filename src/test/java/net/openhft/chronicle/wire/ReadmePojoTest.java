/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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

public class ReadmePojoTest extends WireTestCommon {
    static {
        // Registering 'MyPojos' class for aliasing purposes
        ClassAliasPool.CLASS_ALIASES.addAlias(MyPojos.class);
    }

    @Test
    public void testFromString() throws IOException {
        // Initialize a MyPojos instance with two MyPojo entries
        @NotNull MyPojos mps = new MyPojos("test-list");
        mps.myPojos.add(new MyPojo("text1", 1, 1.1));
        mps.myPojos.add(new MyPojo("text2", 2, 2.2));

       // System.out.println(mps);
       // Convert MyPojos instance to string and back, then validate equality
        @Nullable MyPojos mps2 = Marshallable.fromString(mps.toString());
        assertEquals(mps, mps2);

        // Convert a predefined string into MyPojos object and validate equality
        @NotNull String text = "!MyPojos {\n" +
                "  name: test-list,\n" +
                "  myPojos: [\n" +
                "    { text: text1, num: 1, factor: 1.1 },\n" +
                "    { text: text2, num: 2, factor: 2.2 }\n" +
                "  ]\n" +
                "}\n";
        @Nullable MyPojos mps3 = Marshallable.fromString(text);
        assertEquals(mps, mps3);

        // Read the MyPojos object from a file and validate its content
        @NotNull MyPojos mps4 = Marshallable.fromFile("my-pojos.yaml");
        assertEquals(mps, mps4);
    }

    @Test
    public void testMapDump() throws IOException {
        // Creating a LinkedHashMap with various key-value pairs
        @NotNull Map<String, Object> map = new LinkedHashMap<>();
        map.put("text", "words");
        map.put("number", 1);
        map.put("factor", 1.1);
        map.put("list", Arrays.asList(1L, 2L, 3L, 4L));

        // Create a nested LinkedHashMap
        @NotNull Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("a", 1L);
        inner.put("b", "Hello World");
        inner.put("c", "bye");
        map.put("inner", inner);

        // Convert the map to a string representation
        String text = TEXT.asString(map);
        // Validate the string representation of the map
        assertEquals("text: words\n" +
                "number: !int 1\n" +
                "factor: 1.1\n" +
                "list: [\n" +
                "  1,\n" +
                "  2,\n" +
                "  3,\n" +
                "  4\n" +
                "]\n" +
                "inner: {\n" +
                "  a: 1,\n" +
                "  b: Hello World,\n" +
                "  c: bye\n" +
                "}\n", text);

        // Convert the string back into a map and validate equality
        @Nullable Map<String, Object> map2 = TEXT.asMap(text);
        assertEquals(map, map2);
    }

    static class MyPojo extends SelfDescribingMarshallable {
        String text; // Textual data
        int num;     // Numerical value
        double factor; // Floating point factor

        // Constructor for MyPojo
        MyPojo(String text, int num, double factor) {
            this.text = text;
            this.num = num;
            this.factor = factor;
        }
    }

    static class MyPojos extends SelfDescribingMarshallable {
        String name; // Name of the collection
        @NotNull
        List<MyPojo> myPojos = new ArrayList<>(); // List of MyPojo objects

        // Constructor for MyPojos
        MyPojos(String name) {
            this.name = name;
        }
    }
}
