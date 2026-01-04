/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    @DisplayName("MyClass1 serialises without outer reference field")
    public void test1() {
        MyClass1 instance = new MyClass1(128);
        assertEquals(128L, instance.value, "MyClass1 value should match constructor input for toString");
        assertEquals("!net.openhft.chronicle.wire.marshallable.This0AsTransientTest$MyClass1 {\n" +
                        "  value: 128\n" +
                        "}\n",
                instance.toString(),
                "MyClass1 should serialise without the outer reference field");
    }

    /**
     * Test serialization of MyClass1 with YAML, capturing expected exception due to presence of 'this$0'.
     */
    @Test
    @DisplayName("MyClass1 YAML write ignores this$0 field")
    public void test1b() {
        expectException("Found this$0, in class ");
        Wire wire = WireType.YAML_ONLY.apply(Bytes.allocateElasticOnHeap());
        MyClass1 instance = new MyClass1(1111);
        assertEquals(1111L, instance.value, "MyClass1 value should match constructor input for YAML write");
        wire.writeMessage("test", instance);
        assertEquals("test: !net.openhft.chronicle.wire.marshallable.This0AsTransientTest$MyClass1 {\n" +
                        "  value: 1111\n" +
                        "}\n" +
                        "...\n",
                wire.bytes().toString(),
                "MyClass1 YAML should include the value and omit the outer reference");
    }

    /**
     * Test serialization of MyClass2, which does not explicitly have a 'this$0' field.
     */
    @Test
    @DisplayName("MyClass2 serialises without outer reference field")
    public void test2() {
        MyClass2 instance = new MyClass2(128);
        assertEquals(128L, instance.value, "MyClass2 value should match constructor input for toString");
        assertEquals("!net.openhft.chronicle.wire.marshallable.This0AsTransientTest$MyClass2 {\n" +
                        "  value: 128\n" +
                        "}\n",
                instance.toString(),
                "MyClass2 should serialise without the outer reference field");
    }

    /**
     * Test serialization of MyClass2 with YAML, capturing expected exception due to presence of 'this$0'.
     * MyClass2 has an additional 'this$0' field to demonstrate the presence of this hidden field in inner classes.
     */
    @Test
    @DisplayName("MyClass2 YAML write ignores this$0 fields")
    public void test2b() {
        expectException("Found this$0, in class ");
        expectException("Found this$0$, in class ");
        Wire wire = WireType.YAML_ONLY.apply(Bytes.allocateElasticOnHeap());
        MyClass2 instance = new MyClass2(2222);
        assertEquals(2222L, instance.value, "MyClass2 value should match constructor input for YAML write");
        wire.writeMessage("test", instance);
        assertEquals("test: !net.openhft.chronicle.wire.marshallable.This0AsTransientTest$MyClass2 {\n" +
                        "  value: 2222\n" +
                        "}\n" +
                        "...\n",
                wire.bytes().toString(),
                "MyClass2 YAML should include the value and omit the outer reference");
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
        @SuppressWarnings("checkstyle:MemberName")
        String this$0;
        long value;

        MyClass2(long value) {
            this.value = value;
        }
    }
}
