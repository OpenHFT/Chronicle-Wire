/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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

public class MethodReaderArgumentsRecycleTest {
    private MyInterface writer;
    private MethodReader reader;
    private volatile Object lastArgumentRef;

    @Before
    public void setUp() {
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());

        writer = wire.methodWriter(MyInterface.class);

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

    @Test
    public void testIntArrayNotRecycled() {
        int[] first = {1, 2, 3};
        int[] second = {5, 6, 7, 8};

        writer.intArrayCall(first);
        assertTrue(reader.readOne());
        Object firstRef = lastArgumentRef;
        assertArrayEquals(first, (int[]) lastArgumentRef);

        writer.intArrayCall(second);
        assertTrue(reader.readOne());
        assertArrayEquals(second, (int[]) lastArgumentRef);

        assertNotSame(firstRef, lastArgumentRef);
    }

    @Test
    public void testObjectArrayNotRecycled() {
        String[] first = {"a", "b", "c"};
        String[] second = {"d", ""};

        writer.objectArrayCall(first);
        assertTrue(reader.readOne());
        Object firstRef = lastArgumentRef;
        assertArrayEquals(first, (Object[]) lastArgumentRef);

        writer.objectArrayCall(second);
        assertTrue(reader.readOne());
        assertArrayEquals(second, (Object[]) lastArgumentRef);

        assertNotSame(firstRef, lastArgumentRef);
    }

    @Test
    public void testMarshallableRecycled() {
        MyMarshallable first = new MyMarshallable();
        first.l = 5L;

        MyMarshallable second = new MyMarshallable();
        second.l = 7L;

        writer.marshallableCall(first);
        assertTrue(reader.readOne());
        Object firstRef = lastArgumentRef;
        assertEquals(first, lastArgumentRef);

        writer.marshallableCall(second);
        assertTrue(reader.readOne());
        assertEquals(second, lastArgumentRef);

        assertSame(firstRef, lastArgumentRef);
    }

    @Test
    public void testBytesMarshallableRecycled() {
        MyBytesMarshallable first = new MyBytesMarshallable();
        first.d = 8.5;

        MyBytesMarshallable second = new MyBytesMarshallable();
        second.d = 32.25;

        writer.bytesMarshallableCall(first);
        assertTrue(reader.readOne());
        Object firstRef = lastArgumentRef;
        assertEquals(0, Double.compare(first.d, ((MyBytesMarshallable)lastArgumentRef).d));

        writer.bytesMarshallableCall(second);
        assertTrue(reader.readOne());
        assertEquals(0, Double.compare(second.d, ((MyBytesMarshallable)lastArgumentRef).d));

        assertSame(firstRef, lastArgumentRef);
    }

    @Test
    public void testDtoRecycled() {
        RegularDTO first = new RegularDTO();
        first.i = 6;
        first.s = "f";

        RegularDTO second = new RegularDTO();
        second.i = -3;
        second.s = "s";

        writer.dtoCall(first);
        assertTrue(reader.readOne());
        Object firstRef = lastArgumentRef;
        assertEquals(first.i, ((RegularDTO)lastArgumentRef).i);
        assertEquals(first.s, ((RegularDTO)lastArgumentRef).s);

        writer.dtoCall(second);
        assertTrue(reader.readOne());
        assertEquals(second.i, ((RegularDTO)lastArgumentRef).i);
        assertEquals(second.s, ((RegularDTO)lastArgumentRef).s);

        assertSame(firstRef, lastArgumentRef);
    }

    @Test
    public void testListRecycled() {
        List<String> first = new ArrayList<>();
        first.add("a");
        first.add("b");

        List<String> second = new ArrayList<>();
        second.add("c");
        second.add("d");
        second.add("e");

        writer.listCall(first);
        assertTrue(reader.readOne());
        Object firstRef = lastArgumentRef;
        assertEquals(first, lastArgumentRef);

        writer.listCall(second);
        assertTrue(reader.readOne());
        assertEquals(second, lastArgumentRef);

        assertSame(firstRef, lastArgumentRef);
    }

    @Test
    public void testMapRecycled() {
        Map<String, String> first = new HashMap<>();
        first.put("a", "A");
        first.put("b", "B");

        Map<String, String> second = new HashMap<>();
        second.put("c", "C");
        second.put("d", "D");
        second.put("e", "E");

        writer.mapCall(first);
        assertTrue(reader.readOne());
        Object firstRef = lastArgumentRef;
        assertEquals(first, lastArgumentRef);

        writer.mapCall(second);
        assertTrue(reader.readOne());
        assertEquals(second, lastArgumentRef);

        assertSame(firstRef, lastArgumentRef);
    }

    @Test
    public void testSetRecycled() {
        Set<String> first = new HashSet<>();
        first.add("a");
        first.add("b");

        Set<String> second = new HashSet<>();
        second.add("c");
        second.add("d");
        second.add("e");

        writer.setCall(first);
        assertTrue(reader.readOne());
        Object firstRef = lastArgumentRef;
        assertEquals(first, lastArgumentRef);

        writer.setCall(second);
        assertTrue(reader.readOne());
        assertEquals(second, lastArgumentRef);

        assertSame(firstRef, lastArgumentRef);
    }

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

    public static class MyMarshallable extends SelfDescribingMarshallable {
        long l;
    }

    public static class MyBytesMarshallable implements BytesMarshallable {
        double d;
    }

    public static class RegularDTO {
        String s;
        int i;
    }
}
