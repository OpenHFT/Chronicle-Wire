/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

// This class provides tests for default marshaller functionalities.
class DefaultMarshallerTest extends WireTestCommon {

    // Test the deserialization process with nested arrays.
    @Test
    @DisplayName("Deserialises nested enum arrays with default marshaller")
    void testDeserializeWithNestedArray() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory disabled; skip default marshaller nested array test");

        // Adding class alias for NestedEnum
        ClassAliasPool.CLASS_ALIASES.addAlias(NestedEnum.class);

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
                "}\n", oc.toString(),
                "text form should match expected nested array DTO output");

        // Serializing the object into Wire
        @NotNull Wire text = WireType.TEXT.apply(Bytes.allocateElasticOnHeap(128));
        oc.writeMarshallable(text);

        // Deserializing back into another object
        @NotNull DMOuterClassWithEmbeddedArray oc2 = new DMOuterClassWithEmbeddedArray();
        oc2.readMarshallable(text);

        // Asserting the equality of original and deserialized object
        assertEquals(oc, oc2,
                "deserialised nested array DTO should match original");
        assertEquals("words", oc2.str,
                "deserialised string should match original");
        assertEquals(3, oc2.enums.length,
                "deserialised enum array length should match original");
        assertEquals(NestedEnum.ONE, oc2.enums[0],
                "deserialised enum array element 0 should match original");
        assertEquals(NestedEnum.TWO, oc2.enums[1],
                "deserialised enum array element 1 should match original");
        assertEquals(NestedEnum.THREE, oc2.enums[2],
                "deserialised enum array element 2 should match original");

        // Releasing the memory
        text.bytes().releaseLast();
    }

    // Test the deserialization process.
    @Test
    @DisplayName("Deserialises nested objects and maps with default marshaller")
    void testDeserialize() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory disabled; skip default marshaller object round-trip test");

        // Adding class alias for DMNestedClass
        ClassAliasPool.CLASS_ALIASES.addAlias(DMNestedClass.class);
        DMOuterClass dmOuterClass = ObjectUtils.newInstance(DMOuterClass.class);
        assertNotNull(dmOuterClass.nested,
                "nested list should be initialised for DMOuterClass");

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
                "}\n", oc.toString(),
                "text form should match expected nested object output");

        // Serializing the object into Wire
        @NotNull Wire text = WireType.TEXT.apply(Bytes.allocateElasticOnHeap(64));
        oc.writeMarshallable(text);

        // Deserializing back into another object
        @NotNull DMOuterClass oc2 = new DMOuterClass();
        oc2.readMarshallable(text);

        // Asserting the equality of original and deserialized object
        assertEquals(oc, oc2,
                "deserialised DMOuterClass should match original");
        assertEquals("words", oc2.getText(),
                "deserialised text should match original");
        assertTrue(oc2.isB(), "deserialised boolean flag should match original");
        assertEquals((byte) 1, oc2.getBb(),
                "deserialised byte should match original");
        assertEquals((short) 6, oc2.getS(),
                "deserialised short should match original");
        assertEquals(3.0f, oc2.getF(), 0.0f,
                "deserialised float should match original");
        assertEquals(2.0, oc2.getD(), 0.0,
                "deserialised double should match original");
        assertEquals(5L, oc2.getL(),
                "deserialised long should match original");
        assertEquals(4, oc2.getI(),
                "deserialised int should match original");
        assertEquals(2, oc2.nested.size(),
                "deserialised nested list size should match original");
        assertEquals("hi", oc2.nested.get(0).getStr(),
                "nested list entry 0 should match original text");
        assertEquals(111, oc2.nested.get(0).getNum(),
                "nested list entry 0 should match original number");
        assertEquals("bye", oc2.nested.get(1).getStr(),
                "nested list entry 1 should match original text");
        assertEquals(999, oc2.nested.get(1).getNum(),
                "nested list entry 1 should match original number");
        assertEquals("value", oc2.map.get("key").getStr(),
                "nested map entry key should match original text");
        assertEquals(1, oc2.map.get("key").getNum(),
                "nested map entry key should match original number");
        assertEquals("valuez", oc2.map.get("keyz").getStr(),
                "nested map entry keyz should match original text");
        assertEquals(1111, oc2.map.get("keyz").getNum(),
                "nested map entry keyz should match original number");

        // Releasing the memory
        text.bytes().releaseLast();
    }

    // Defining the NestedEnum
    enum NestedEnum {
        ONE,
        TWO,
        THREE
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
