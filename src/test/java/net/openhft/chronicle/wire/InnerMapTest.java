//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assume.assumeFalse;

public class InnerMapTest extends WireTestCommon {

    // A test case to verify the marshaling and demarshaling of the `MyMarshable` class
    @Test
    public void testMyInnnerMap() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        // Create a new instance of MyMarshable and set its properties
        @NotNull MyMarshable myMarshable = new MyMarshable("rob");
        myMarshable.commission().put("hello", 123.4);
        myMarshable.nested = new MyNested("text");

        // Convert the instance to a string representation
        String asString = myMarshable.toString();
        // Assert that the string representation matches the expected format
        Assert.assertEquals("!net.openhft.chronicle.wire.InnerMapTest$MyMarshable {\n" +
                "  name: rob,\n" +
                "  commission: {\n" +
                "    hello: 123.4\n" +
                "  },\n" +
                "  nested: !net.openhft.chronicle.wire.InnerMapTest$MyNested {\n" +
                "    value: text\n" +
                "  }\n" +
                "}\n", asString);

        // Allocate elastic byte buffer to hold serialized data
        @SuppressWarnings("rawtypes")
        Bytes<?> b = Bytes.allocateElasticOnHeap();

        // Create a binary wire object for serialization; note the comment about binary vs text
        @NotNull Wire w = new BinaryWire(b);
        w.usePadding(true);

        // Write the MyMarshable instance to the wire
        try (DocumentContext dc = w.writingDocument(false)) {
            dc.wire().write("marshable").typedMarshallable(myMarshable);
        }

        // Read the MyMarshable instance from the wire and assert its string representation
        try (DocumentContext dc = w.readingDocument()) {
            @Nullable MyMarshable tm = dc.wire().read(() -> "marshable").typedMarshallable();
            Assert.assertEquals(asString, tm.toString());
        }

        // Release the byte buffer resources
        b.releaseLast();
    }

    // A class representing a marshallable object with a name, commission map, and nested object
    static class MyMarshable extends SelfDescribingMarshallable implements Demarshallable {
        String name;
        Map<String, Double> commission;
        Marshallable nested;

        // Constructor used for deserialization
        @UsedViaReflection
        public MyMarshable(@NotNull WireIn wire) {
            readMarshallable(wire);
        }

        // Default constructor
        MyMarshable(String name) {
            this.name = name;
            this.commission = new LinkedHashMap<>();
        }

        // Getter for name
        public String name() {
            return name;
        }

        // Getter for commission map
        Map<String, Double> commission() {
            return commission;
        }

        // Setter for name with fluent interface design
        @NotNull
        public MyMarshable name(String name) {
            this.name = name;
            return this;
        }
    }

    // A nested class within the `MyMarshable` class
    static class MyNested extends SelfDescribingMarshallable {
        String value;

        // Constructor to initialize the value
        MyNested(String value) {
            this.value = value;
        }
    }
}
