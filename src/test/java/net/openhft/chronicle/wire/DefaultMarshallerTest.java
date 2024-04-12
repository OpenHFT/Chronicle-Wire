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
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

// This class provides tests for default marshaller functionalities.
public class DefaultMarshallerTest extends WireTestCommon {

    // Test the deserialization process with nested arrays.
    @Test
    public void testDeserializeWithNestedArray() {
        // Adding class alias for NestedEnum
        ClassAliasPool.CLASS_ALIASES.addAlias(NestedEnum.class);
        DMOuterClassWithEmbeddedArray dmOuterClass = ObjectUtils.newInstance(DMOuterClassWithEmbeddedArray.class);

        // Creating an instance of DMOuterClassWithEmbeddedArray
        @NotNull DMOuterClassWithEmbeddedArray oc = new DMOuterClassWithEmbeddedArray("words");
        oc.enums = new NestedEnum[3];
        oc.enums[0] = NestedEnum.ONE;
        oc.enums[1] = NestedEnum.TWO;
        oc.enums[2] = NestedEnum.THREE;

        // Asserting the expected output string representation of the object
        assertEquals("!net.openhft.chronicle.wire.DefaultMarshallerTest$DMOuterClassWithEmbeddedArray {\n" +
                "  str: words,\n" +
                "  enums: [ ONE, TWO, THREE ]\n" +
                "}\n", oc.toString());

        // Serializing the object into Wire
        @NotNull Wire text = WireType.TEXT.apply(Bytes.allocateElasticOnHeap(128));
        oc.writeMarshallable(text);

        // Deserializing back into another object
        @NotNull DMOuterClassWithEmbeddedArray oc2 = new DMOuterClassWithEmbeddedArray();
        oc2.readMarshallable(text);

        // Asserting the equality of original and deserialized object
        assertEquals(oc, oc2);

        // Releasing the memory
        text.bytes().releaseLast();
    }

    // Test the deserialization process.
    @Test
    public void testDeserialize() {
        // Adding class alias for DMNestedClass
        ClassAliasPool.CLASS_ALIASES.addAlias(DMNestedClass.class);
        DMOuterClass dmOuterClass = ObjectUtils.newInstance(DMOuterClass.class);
        assertNotNull(dmOuterClass.nested);

        // Creating an instance of DMOuterClass
        @NotNull DMOuterClass oc = new DMOuterClass("words", true, (byte) 1, 2, 3, 4, 5, (short) 6);
        oc.nested.add(new DMNestedClass("hi", 111));
        oc.nested.add(new DMNestedClass("bye", 999));
        oc.map.put("key", new DMNestedClass("value", 1));
        oc.map.put("keyz", new DMNestedClass("valuez", 1111));

        // Asserting the expected output string representation of the object
        assertEquals("!net.openhft.chronicle.wire.DMOuterClass {\n" +
                "  text: words,\n" +
                "  b: true,\n" +
                "  bb: 1,\n" +
                "  s: 6,\n" +
                "  f: 3.0,\n" +
                "  d: 2.0,\n" +
                "  l: 5,\n" +
                "  i: 4,\n" +
                "  nested: [\n" +
                "    { str: hi, num: 111 },\n" +
                "    { str: bye, num: 999 }\n" +
                "  ],\n" +
                "  map: {\n" +
                "    key: { str: value, num: 1 },\n" +
                "    keyz: { str: valuez, num: 1111 }\n" +
                "  }\n" +
                "}\n", oc.toString());

        // Serializing the object into Wire
        @NotNull Wire text = WireType.TEXT.apply(Bytes.allocateElasticOnHeap(64));
        oc.writeMarshallable(text);

        // Deserializing back into another object
        @NotNull DMOuterClass oc2 = new DMOuterClass();
        oc2.readMarshallable(text);

        // Asserting the equality of original and deserialized object
        assertEquals(oc, oc2);

        // Releasing the memory
        text.bytes().releaseLast();
    }

    // Defining the NestedEnum
    enum NestedEnum {
        ONE,
        TWO,
        THREE;
    }

    // Defining a class with an embedded array
    static class DMOuterClassWithEmbeddedArray extends SelfDescribingMarshallable {
        String str;
        NestedEnum[] enums;

        DMOuterClassWithEmbeddedArray() {

        }

        DMOuterClassWithEmbeddedArray(String s) {
            this.str = s;
        }

        @Override
        public boolean equals(Object o) {
            return super.equals(o);
        }
    }
}
