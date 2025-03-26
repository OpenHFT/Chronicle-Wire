/*
 * Copyright 2016-2020 chronicle.software
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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class MethodReaderArgumentsRecycleTest extends WireTestCommon {

    // Interface that represents the different method signatures we want to test.
    private MyInterface writer;

    // MethodReader will be used to simulate method calls on the MyInterface implementation.
    private MethodReader reader;

    // Will hold the last argument received in a method call.
    private volatile Object lastArgumentRef;

    // This method sets up the test environment before each test case.
    @SuppressWarnings("deprecation")
    @Before
    public void setUp() {
        // Create a new BinaryWire backed by a dynamically expanding Bytes object.
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        wire.usePadding(true);

        // Create a proxy instance of MyInterface that will write method calls to the wire.
        writer = wire.methodWriter(MyInterface.class);

        // Create a MethodReader to read method calls from the wire and dispatch them to
        // the provided MyInterface implementation.
        reader = wire.methodReader(new MyInterface() {
            @Override
            public void intArrayCall(int[] a) {
                lastArgumentRef = a;
            }

            @Override
            public void objectArrayCall(String[] o) {
                lastArgumentRef = o;
            }

            @Override
            public void marshallableCall(MyMarshallable m) {
                lastArgumentRef = m;
            }

            @Override
            public void bytesMarshallableCall(MyBytesMarshallable b) {
                lastArgumentRef = b;
            }

            @Override
            public void dtoCall(RegularDTO d) {
                lastArgumentRef = d;
            }

            @Override
            public void configDtoCall(ConfigDTO d) {
                lastArgumentRef = d;
            }

            @Override
            public void wrappedListCall(ListContainingDto ld) {
                lastArgumentRef = ld;
            }

            @Override
            public void wrappedObjectCall(ObjectContainingDto ld) {
                lastArgumentRef = ld;
            }

            @Override
            public void listCall(List<?> c) {
                lastArgumentRef = c;
            }

            @Override
            public void mapCall(Map<?, ?> m) {
                lastArgumentRef = m;
            }

            @Override
            public void setCall(Set<?> s) {
                lastArgumentRef = s;
            }
        });
    }

    private static void _assertEquals(Object a, Object b) {
        if (a instanceof int[] && b instanceof int[])
            assertArrayEquals((int[]) a, (int[]) b);
        else if (a instanceof Object[] && b instanceof Object[])
            assertArrayEquals((Object[]) a, (Object[]) b);
        else
            assertEquals(a, b);
    }

    // Utility method to verify that an argument is not recycled between method calls.
    private <T> void verifyNotRecycled(T firstArg, T secondArg, Consumer<T> call) {
        call.accept(firstArg);
        assertTrue(reader.readOne());
        Object firstRef = lastArgumentRef;
        _assertEquals(firstArg, lastArgumentRef);

        call.accept(secondArg);
        assertTrue(reader.readOne());
        _assertEquals(secondArg, lastArgumentRef);

        assertNotSame(firstRef, lastArgumentRef);
    }

    // Utility method to verify that an argument is recycled between method calls.
    private <T> void verifyRecycled(T firstArg, T secondArg, Consumer<T> call) {
        call.accept(firstArg);
        assertTrue(reader.readOne());
        Object firstRef = lastArgumentRef;
        assertEquals(firstArg, lastArgumentRef);

        call.accept(secondArg);
        assertTrue(reader.readOne());
        assertEquals(secondArg, lastArgumentRef);

        assertSame(firstRef, lastArgumentRef);
    }

    @Test
    public void testIntArrayNotRecycled() {
        // Two different int arrays to pass to the method.
        int[] first = {1, 2, 3};
        int[] second = {5, 6, 7, 8};

        verifyNotRecycled(first, second, writer::intArrayCall);
    }

    // Test to ensure that an Object array argument is not recycled between calls.
    @Test
    public void testObjectArrayNotRecycled() {
        String[] first = {"a", "b", "c"};
        String[] second = {"d", ""};

        verifyNotRecycled(first, second, writer::objectArrayCall);
    }

    // Test to verify that a MyMarshallable object argument gets recycled between calls.
    @Test
    public void testMarshallableRecycled() {
        MyMarshallable first = new MyMarshallable();
        first.l = 5L;

        MyMarshallable second = new MyMarshallable();
        second.l = 7L;

        verifyRecycled(first, second, writer::marshallableCall);
    }

    // Test to confirm that a MyBytesMarshallable object argument gets recycled between calls.
    @Test
    public void testBytesMarshallableRecycled() {
        MyBytesMarshallable first = new MyBytesMarshallable();
        first.d = 8.5;

        MyBytesMarshallable second = new MyBytesMarshallable();
        second.d = 32.25;

        verifyRecycled(first, second, writer::bytesMarshallableCall);
    }

    // Test to ascertain that a RegularDTO object argument gets recycled between calls.
    @Test
    public void testDtoRecycled() {
        RegularDTO first = new RegularDTO();
        first.i = 6;
        first.s = "f";

        RegularDTO second = new RegularDTO();
        second.i = -3;
        second.s = "s";

        verifyRecycled(first, second, writer::dtoCall);
    }

    @Test
    public void testConfigDtoRecycled() {
        ConfigDTO first = new ConfigDTO();
        first.s = "f";
        first.b = true;

        ConfigDTO second = new ConfigDTO();
        second.s = "s";
        // don't set b

        verifyRecycled(first, second, writer::configDtoCall);

        ConfigDTO dto = (ConfigDTO) lastArgumentRef;
        Assert.assertFalse(dto.b);
    }

    // Test to ascertain that a DTO object's list field gets recycled between calls.
    @Test
    public void testWrappedListRecycled() {
        ListContainingDto first = new ListContainingDto();
        first.list = new ArrayList<>(Arrays.asList(6, "f"));

        ListContainingDto second = new ListContainingDto();
        second.list = new ArrayList<>(Arrays.asList(-3, "s"));

        verifyRecycled(first, second, writer::wrappedListCall);
    }

    // Test to ascertain that a DTO object's list field gets recycled between calls.
    @Test
    public void testWrappedListAsObjectRecycled() {
        ObjectContainingDto first = new ObjectContainingDto();
        first.list = new ArrayList<>(Arrays.asList(6, "f"));

        ObjectContainingDto second = new ObjectContainingDto();
        second.list = new ArrayList<>(Arrays.asList(-3, "s"));

        verifyRecycled(first, second, writer::wrappedObjectCall);
    }

    // Test to ascertain that a DTO object's list field gets recycled between calls.
    @Test
    public void testWrappedListAsObjectRecycledDTO() {
        ObjectContainingDto first = new ObjectContainingDto();
        first.list = new ArrayList<>(Arrays.asList(new MyDto(1, 2), new MyDto(3, 4), new MyDto(5, 6)));

        ObjectContainingDto second = new ObjectContainingDto();
        second.list = new ArrayList<>(Arrays.asList(new MyDto(7, 8), new MyDto(9, 0)));

        // Make a call with the first ListContainingDto and read the response.
        writer.wrappedObjectCall(first);
        assertTrue(reader.readOne());
        Object firstRef = lastArgumentRef;
        assertEquals("" +
                "!net.openhft.chronicle.wire.MethodReaderArgumentsRecycleTest$ObjectContainingDto {\n" +
                "  list: [\n" +
                "    !net.openhft.chronicle.wire.MethodReaderArgumentsRecycleTest$MyDto { a: 1, b: 2 },\n" +
                "    !net.openhft.chronicle.wire.MethodReaderArgumentsRecycleTest$MyDto { a: 3, b: 4 },\n" +
                "    !net.openhft.chronicle.wire.MethodReaderArgumentsRecycleTest$MyDto { a: 5, b: 6 }\n" +
                "  ]\n" +
                "}\n", lastArgumentRef.toString());
        List<?> list1 = (List<?>) ((ObjectContainingDto) lastArgumentRef).list;
        assertEquals(first.list, list1);
        MyDto dto0 = (MyDto) list1.get(0);
        MyDto dto1 = (MyDto) list1.get(1);

        // Make a call with the second ListContainingDto and read the response.
        writer.wrappedObjectCall(second);
        assertTrue(reader.readOne());
        assertEquals("!net.openhft.chronicle.wire.MethodReaderArgumentsRecycleTest$ObjectContainingDto {\n" +
                "  list: [\n" +
                "    !net.openhft.chronicle.wire.MethodReaderArgumentsRecycleTest$MyDto { a: 7, b: 8 },\n" +
                "    !net.openhft.chronicle.wire.MethodReaderArgumentsRecycleTest$MyDto { a: 9, b: 0 }\n" +
                "  ]\n" +
                "}\n", lastArgumentRef.toString());
        List<?> list2 = (List<?>) ((ObjectContainingDto) lastArgumentRef).list;
        assertEquals(second.list, list2);
        assertSame(dto0, list1.get(0));
        assertSame(dto1, list1.get(1));

        // Ensure the reference from the first call is the same as the second.
        assertSame(firstRef, lastArgumentRef);
    }

    // Test to ensure that a List argument gets recycled between calls.
    @Test
    public void testListRecycled() {
        List<String> first = new ArrayList<>();
        first.add("a");
        first.add("b");

        List<String> second = new ArrayList<>();
        second.add("c");
        second.add("d");
        second.add("e");

        verifyRecycled(first, second, writer::listCall);
    }

    // Test to confirm that a Map argument gets recycled between calls.
    @Test
    public void testMapRecycled() {
        Map<String, String> first = new HashMap<>();
        first.put("a", "A");
        first.put("b", "B");

        Map<String, String> second = new HashMap<>();
        second.put("c", "C");
        second.put("d", "D");
        second.put("e", "E");

        verifyRecycled(first, second, writer::mapCall);
    }

    // Test to confirm that a Set argument gets recycled between calls.
    @Test
    public void testSetRecycled() {
        Set<String> first = new HashSet<>();
        first.add("a");
        first.add("b");

        Set<String> second = new HashSet<>();
        second.add("c");
        second.add("d");
        second.add("e");

        verifyRecycled(first, second, writer::setCall);
    }

    // Interface definition for various types of method calls.
    interface MyInterface {
        void intArrayCall(int[] a);

        void objectArrayCall(String[] o);

        void marshallableCall(MyMarshallable m);

        void bytesMarshallableCall(MyBytesMarshallable b);

        void dtoCall(RegularDTO d);

        void configDtoCall(ConfigDTO d);

        void wrappedListCall(ListContainingDto ld);

        void wrappedObjectCall(ObjectContainingDto ld);

        void listCall(List<?> c);

        void mapCall(Map<?, ?> m);

        void setCall(Set<?> s);
    }

    // Definition of a custom Marshallable object with a long field.
    public static class MyMarshallable extends SelfDescribingMarshallable {
        long l;
    }

    // Definition of a custom BytesMarshallable object with a double field.
    public static class MyBytesMarshallable extends BytesInBinaryMarshallable {
        double d;
    }

    // Definition of a regular Data Transfer Object (DTO) with string and integer fields.
    public static class RegularDTO extends SelfDescribingMarshallable {
        String s;
        int i;
    }

    public static class ConfigDTO extends AbstractMarshallableCfg {
        String s;
        boolean b;
    }

    // Definition of a DTO with a list-containing field.
    public static class ListContainingDto extends SelfDescribingMarshallable {
        List<Object> list;
    }

    // Definition of a DTO with a list-containing field.
    public static class ObjectContainingDto extends SelfDescribingMarshallable {
        Object list;
    }

    public static class MyDto extends SelfDescribingMarshallable {
        private final int a;
        private final int b;

        public MyDto(int a, int b) {
            this.a = a;
            this.b = b;
        }
    }
}
