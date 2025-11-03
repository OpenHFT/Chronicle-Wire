/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests to demonstrate the serialization of inner classes that might have 'this$0' fields.
 * Non-static inner classes in Java have a hidden field named 'this$0' which is a reference
 * to the outer instance. This class tests the serialization behavior with respect to this field.
 */
public class This0AsTransientTest extends WireTestCommon {

    /**
     * Test serialization of MyClass1, which does not explicitly have a 'this$0' field.
     */
    @Test
    public void test1() {
        assertEquals("" +
                        "!net.openhft.chronicle.wire.marshallable.This0AsTransientTest$MyClass1 {\n" +
                        "  value: 128\n" +
                        "}\n",
                new MyClass1(128).toString());
    }

    /**
     * Test serialization of MyClass1 with YAML, capturing expected exception due to presence of 'this$0'.
     */
    @Test
    public void test1b() {
        expectException("Found this$0, in class ");
        Wire wire = WireType.YAML_ONLY.apply(Bytes.allocateElasticOnHeap());
        wire.writeMessage("test", new MyClass1(1111));
        assertEquals("" +
                        "test: !net.openhft.chronicle.wire.marshallable.This0AsTransientTest$MyClass1 {\n" +
                        "  value: 1111\n" +
                        "}\n" +
                        "...\n",
                wire.bytes().toString());
    }

    /**
     * Test serialization of MyClass2, which does not explicitly have a 'this$0' field.
     */
    @Test
    public void test2() {
        assertEquals("" +
                        "!net.openhft.chronicle.wire.marshallable.This0AsTransientTest$MyClass2 {\n" +
                        "  value: 128\n" +
                        "}\n",
                new MyClass2(128).toString());
    }

    /**
     * Test serialization of MyClass2 with YAML, capturing expected exception due to presence of 'this$0'.
     * MyClass2 has an additional 'this$0' field to demonstrate the presence of this hidden field in inner classes.
     */
    @Test
    public void test2b() {
        expectException("Found this$0, in class ");
        expectException("Found this$0$, in class ");
        Wire wire = WireType.YAML_ONLY.apply(Bytes.allocateElasticOnHeap());
        wire.writeMessage("test", new MyClass2(2222));
        assertEquals("" +
                        "test: !net.openhft.chronicle.wire.marshallable.This0AsTransientTest$MyClass2 {\n" +
                        "  value: 2222\n" +
                        "}\n" +
                        "...\n",
                wire.bytes().toString());
    }

    /**
     * Non-static inner class, which inherently has a hidden reference to the outer instance (this$0).
     */
    class MyClass1 extends SelfDescribingMarshallable {
        long value;

        MyClass1(long value) {
            this.value = value;
        }
    }

    /**
     * Another non-static inner class, which also has a hidden reference to the outer instance.
     * This class has an explicit 'this$0' field to mimic the behavior of hidden fields in inner classes.
     */
    class MyClass2 extends SelfDescribingMarshallable {
        String this$0;
        long value;

        MyClass2(long value) {
            this.value = value;
        }
    }
}
