/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

// Test class for testing JSON wire DTO functionalities.
public class JSONWireDTOTest extends WireTestCommon {

    // Test to verify serialization and deserialization of DTO using JSONWire.
    @Test
    public void dto() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        expectException("Found this$0, in class");

        // Allocate bytes for storing the serialized DTO.
        Bytes<?> bytes = Bytes.allocateElasticDirect();

        // Instantiate JSONWire for serialization and deserialization.
        final JSONWire wire = new JSONWire(bytes);

        // Create a test object.
        JSOuterClass dto = new JSOuterClass();
        dto.text = "hi";
        dto.d = 3.1415;
        dto.nested.add(new JSNestedClass("there", 1));

        // Serialize the DTO object to JSON format.
        wire.getValueOut().marshallable(dto);

        // Check the serialized output.
        assertEquals("{\"text\":\"hi\",\"nested\":[ {\"str\":\"there\",\"num\":1} ],\"b\":false,\"bb\":0,\"s\":0,\"f\":0.0,\"d\":3.1415,\"l\":0,\"i\":0}",
                bytes.toString());

        // Create another DTO instance for deserialization.
        JSOuterClass dto2 = new JSOuterClass();

        // Deserialize the JSON data into DTO object.
        wire.getValueIn().marshallable(dto2);

        // Check the deserialized object's string representation.
        assertEquals("!net.openhft.chronicle.wire.JSONWireDTOTest$JSOuterClass {\n" +
                "  text: hi,\n" +
                "  nested: [\n" +
                "    { str: there, num: 1 }\n" +
                "  ],\n" +
                "  b: false,\n" +
                "  bb: 0,\n" +
                "  s: 0,\n" +
                "  f: 0.0,\n" +
                "  d: 3.1415,\n" +
                "  l: 0,\n" +
                "  i: 0\n" +
                "}\n", dto2.toString());

        // Release the allocated bytes.
        bytes.releaseLast();
    }

    // Class representing the outer structure of the DTO.
    @UsedViaReflection
    static class JSOuterClass extends SelfDescribingMarshallable {
        String text;
        @NotNull
        List<JSNestedClass> nested = new ArrayList<>();
        boolean b;
        byte bb;
        short s;
        float f;
        double d;
        long l;
        int i;

        // Default constructor.
        JSOuterClass() {
        }
    }

    // Nested class representing a part of the DTO.
    @UsedViaReflection
    class JSNestedClass extends SelfDescribingMarshallable {
        // must non static and have this$0 for this tests
        String str;
        int num;

        // Constructor to initialize the nested class.
        JSNestedClass(String str, int num) {
            this.str = str;
            this.num = num;
        }
    }
}
