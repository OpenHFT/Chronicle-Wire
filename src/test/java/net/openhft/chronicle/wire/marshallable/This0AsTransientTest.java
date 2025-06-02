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

        public MyClass1(long value) {
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

        public MyClass2(long value) {
            this.value = value;
        }
    }
}
