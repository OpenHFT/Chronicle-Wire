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
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class MethodReaderArgumentsRecycleTest extends WireTestCommon {

    // Interface that represents the different method signatures we want to test.
    private MyInterface writer;

    // MethodReader will be used to simulate method calls on the MyInterface implementation.
    private MethodReader reader;

    // Will hold the last argument received in a method call.
    private volatile Object lastArgumentRef;

    // This method sets up the test environment before each test case.
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
            public void objectArrayCall(Object[] o) {
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

    // This test checks if int arrays passed to the `intArrayCall` method are recycled or not.
    @Test
    public void testIntArrayNotRecycled() {
        // Two different int arrays to pass to the method.
        int[] first = {1, 2, 3};
        int[] second = {5, 6, 7, 8};

        // Write the first method call to the wire.
        writer.intArrayCall(first);

        // Read the method call and dispatch it to the MyInterface implementation.
        assertTrue(reader.readOne());

        // Store the reference of the argument from the first call.
        Object firstRef = lastArgumentRef;

        // Ensure that the method was called with the expected argument.
        assertArrayEquals(first, (int[]) lastArgumentRef);

        // Repeat the process for the second array.
        writer.intArrayCall(second);
        assertTrue(reader.readOne());
        assertArrayEquals(second, (int[]) lastArgumentRef);

        assertNotSame(firstRef, lastArgumentRef);
    }

    // Test to ensure that an Object array argument is not recycled between calls.
    @Test
    public void testObjectArrayNotRecycled() {
        String[] first = {"a", "b", "c"};
        String[] second = {"d", ""};

        // Make a call with the first array and read the response.
        writer.objectArrayCall(first);
        assertTrue(reader.readOne());
        Object firstRef = lastArgumentRef;
        assertArrayEquals(first, (Object[]) lastArgumentRef);

        // Make a call with the second array and read the response.
        writer.objectArrayCall(second);
        assertTrue(reader.readOne());
        assertArrayEquals(second, (Object[]) lastArgumentRef);

        // Ensure the reference from the first call is not the same as the second.
        assertNotSame(firstRef, lastArgumentRef);
    }

    // Test to verify that a MyMarshallable object argument gets recycled between calls.
    @Test
    public void testMarshallableRecycled() {
        MyMarshallable first = new MyMarshallable();
        first.l = 5L;

        MyMarshallable second = new MyMarshallable();
        second.l = 7L;

        // Make a call with the first MyMarshallable and read the response.
        writer.marshallableCall(first);
        assertTrue(reader.readOne());
        Object firstRef = lastArgumentRef;
        assertEquals(first, lastArgumentRef);

        // Make a call with the second MyMarshallable and read the response.
        writer.marshallableCall(second);
        assertTrue(reader.readOne());
        assertEquals(second, lastArgumentRef);

        // Ensure the reference from the first call is the same as the second.
        assertSame(firstRef, lastArgumentRef);
    }

    // Test to confirm that a MyBytesMarshallable object argument gets recycled between calls.
    @Test
    public void testBytesMarshallableRecycled() {
        MyBytesMarshallable first = new MyBytesMarshallable();
        first.d = 8.5;

        MyBytesMarshallable second = new MyBytesMarshallable();
        second.d = 32.25;

        // Make a call with the first MyBytesMarshallable and read the response.
        writer.bytesMarshallableCall(first);
        assertTrue(reader.readOne());
        Object firstRef = lastArgumentRef;
        assertEquals(0, Double.compare(first.d, ((MyBytesMarshallable)lastArgumentRef).d));

        // Make a call with the second MyBytesMarshallable and read the response.
        writer.bytesMarshallableCall(second);
        assertTrue(reader.readOne());
        assertEquals(0, Double.compare(second.d, ((MyBytesMarshallable)lastArgumentRef).d));

        // Ensure the reference from the first call is the same as the second.
        assertSame(firstRef, lastArgumentRef);
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

        // Make a call with the first RegularDTO and read the response.
        writer.dtoCall(first);
        assertTrue(reader.readOne());
        Object firstRef = lastArgumentRef;
        assertEquals(first.i, ((RegularDTO)lastArgumentRef).i);
        assertEquals(first.s, ((RegularDTO)lastArgumentRef).s);

        // Make a call with the second RegularDTO and read the response.
        writer.dtoCall(second);
        assertTrue(reader.readOne());
        assertEquals(second.i, ((RegularDTO)lastArgumentRef).i);
        assertEquals(second.s, ((RegularDTO)lastArgumentRef).s);

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

        // Make a call with the first list and read the response.
        writer.listCall(first);
        assertTrue(reader.readOne());
        Object firstRef = lastArgumentRef;
        assertEquals(first, lastArgumentRef);

        // Make a call with the second list and read the response.
        writer.listCall(second);
        assertTrue(reader.readOne());
        assertEquals(second, lastArgumentRef);

        // Ensure the reference from the first call is the same as the second.
        assertSame(firstRef, lastArgumentRef);
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

        // Make a call with the first map and read the response.
        writer.mapCall(first);
        assertTrue(reader.readOne());
        Object firstRef = lastArgumentRef;
        assertEquals(first, lastArgumentRef);

        // Make a call with the second map and read the response.
        writer.mapCall(second);
        assertTrue(reader.readOne());
        assertEquals(second, lastArgumentRef);

        // Ensure the reference from the first call is the same as the second.
        assertSame(firstRef, lastArgumentRef);
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

        // Make a call with the first set and read the response.
        writer.setCall(first);
        assertTrue(reader.readOne());
        Object firstRef = lastArgumentRef;
        assertEquals(first, lastArgumentRef);

        // Make a call with the second set and read the response.
        writer.setCall(second);
        assertTrue(reader.readOne());
        assertEquals(second, lastArgumentRef);

        // Ensure the reference from the first call is the same as the second.
        assertSame(firstRef, lastArgumentRef);
    }

    // Interface definition for various types of method calls.
    interface MyInterface {
        void intArrayCall(int[] a);

        void objectArrayCall(Object[] o);

        void marshallableCall(MyMarshallable m);

        void bytesMarshallableCall(MyBytesMarshallable b);

        void dtoCall(RegularDTO d);

        void listCall(List<?> c);

        void mapCall(Map<?, ?> m);

        void setCall(Set<?> s);
    }

    // Definition of a custom Marshallable object with a long field.
    public static class MyMarshallable extends SelfDescribingMarshallable {
        long l;
    }

    // Definition of a custom BytesMarshallable object with a double field.
    public static class MyBytesMarshallable implements BytesMarshallable {
        double d;
    }

    // Definition of a regular Data Transfer Object (DTO) with string and integer fields.
    public static class RegularDTO {
        String s;
        int i;
    }
}
