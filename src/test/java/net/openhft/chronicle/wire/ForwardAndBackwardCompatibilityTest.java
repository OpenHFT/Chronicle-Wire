/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;
import static org.junit.jupiter.api.Assertions.*;

// Using the Parameterized runner for JUnit tests to enable parameter-driven tests
class ForwardAndBackwardCompatibilityTest extends WireTestCommon {

    // Holds the WireType for this test instance
    private WireType wireType;

    // Provides the set of WireTypes to be used as parameters for the tests
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // {WireType.TEXT},
                {WireType.BINARY}
        });
    }

    // Test for checking backward compatibility of DTO classes
    @ParameterizedTest
    @MethodSource("data")
    void backwardsCompatibility(WireType wireType) {
        this.wireType = wireType;
        // Expecting an exception due to class replacement
        expectException("Replaced class net.openhft.chronicle.wire.ForwardAndBackwardCompatibilityTest$DTO1 with class net.openhft.chronicle.wire.ForwardAndBackwardCompatibilityTest$DTO2");

        // Creating a Wire instance based on the provided WireType
        Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        wire.usePadding(wire.isBinary());
        CLASS_ALIASES.addAlias(DTO1.class, "DTO");

        // Writing a document with DTO1 data
        wire.writeDocument(false, w -> w.getValueOut().typedMarshallable(new DTO1(1)));
        // System.out.println(Wires.fromSizePrefixedBlobs(wire));

        // Switching the alias to DTO2 class
        CLASS_ALIASES.addAlias(DTO2.class, "DTO");
        if (wire instanceof TextWire)
            ((TextWire) wire).useBinaryDocuments();

        // Reading the written document and expecting to get DTO2 instance
        try (DocumentContext dc = wire.readingDocument()) {
            if (!dc.isPresent())
                fail();
            @Nullable DTO2 dto2 = dc.wire().getValueIn().typedMarshallable();
            assertEquals(1, dto2.one);
            assertEquals(0, dto2.two);
            assertNull(dto2.three);
        }

        // Releasing memory
        wire.bytes().releaseLast();
    }

    // Test for checking forward compatibility of DTO classes
    @ParameterizedTest
    @MethodSource("data")
    void forwardCompatibility(WireType wireType) {
        this.wireType = wireType;
        // Expecting an exception due to class replacement
        expectException("Replaced class net.openhft.chronicle.wire.ForwardAndBackwardCompatibilityTest$DTO2 with class net.openhft.chronicle.wire.ForwardAndBackwardCompatibilityTest$DTO1");

        // Creating a Wire instance based on the provided WireType
        Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        wire.usePadding(wire.isBinary());
        CLASS_ALIASES.addAlias(DTO2.class, "DTO");

        // Writing a document with DTO2 data
        wire.writeDocument(false, w -> w.getValueOut().typedMarshallable(new DTO2(1, 2, 3)));
        // System.out.println(Wires.fromSizePrefixedBlobs(wire));

        // Switching the alias to DTO1 class
        CLASS_ALIASES.addAlias(DTO1.class, "DTO");
        if (wire instanceof TextWire)
            ((TextWire) wire).useBinaryDocuments();

        // Reading the written document and expecting to get DTO1 instance
        try (DocumentContext dc = wire.readingDocument()) {
            if (!dc.isPresent())
                fail();
            @Nullable DTO1 dto1 = dc.wire().getValueIn().typedMarshallable();
            assertEquals(1, dto1.one);
        }

        // Releasing memory
        wire.bytes().releaseLast();
    }

    // Test to ensure that new data added to a document doesn't affect old reads
    @ParameterizedTest
    @MethodSource("data")
    void testCheckThatNewDataAddedToADocumentDoesNotEffectOldReads(WireType wireType) {
        this.wireType = wireType;

        Bytes<?> b = Bytes.allocateElasticOnHeap();
        try {
            // Creating a Wire instance
            Wire w = WireType.FIELDLESS_BINARY.apply(b);
            w.usePadding(true);

            // Writing two documents with different sets of data
            try (DocumentContext dc = w.writingDocument()) {
                dc.wire().write("hello").text("hello world");
                dc.wire().write("hello2").text("hello world");
            }

            try (DocumentContext dc = w.writingDocument()) {
                dc.wire().write("other data").text("other data");
            }

            // Reading back the documents and verifying the data
            try (DocumentContext dc = w.readingDocument()) {
                assertEquals("hello world", dc.wire().read("hello").text());
            }

            try (DocumentContext dc = w.readingDocument()) {
                assertEquals("other data", dc.wire().read("other data").text());
            }
        } finally {
            // Releasing memory
            b.releaseLast();
        }
    }

    @SuppressWarnings("this-escape")
    static class DTO1 extends SelfDescribingMarshallable implements Demarshallable {

        // Field to hold an integer value
        int one;

        // Constructor used via reflection for deserialization
        @UsedViaReflection
        DTO1(@NotNull WireIn wire) {
            readMarshallable(wire);
        }

        // Regular constructor to initialize 'one' field
        DTO1(int i) {
            this.one = i;
        }

        // Getter method for 'one' field
        public int one() {
            return one;
        }

        // Fluent setter method for 'one' field
        @NotNull
        public DTO1 one(int one) {
            this.one = one;
            return this;
        }
    }

    @SuppressWarnings("this-escape")
    static class DTO2 extends SelfDescribingMarshallable implements Demarshallable {
        // Field to hold an Object
        Object three;
        // Field to hold an integer value
        int one;
        // Another field to hold an integer value
        int two;
        // Unused field to hold an Object
        Object o;

        // Constructor used via reflection for deserialization
        @UsedViaReflection
        DTO2(@NotNull WireIn wire) {
            readMarshallable(wire);
        }

        // Regular constructor to initialize fields 'one', 'two', and 'three'
        DTO2(int one, int two, Object three) {
            this.one = one;
            this.two = two;
            this.three = three;
        }

        // Getter method for 'three' field
        public Object three() {
            return three;
        }

        // Fluent setter method for 'three' field
        @NotNull
        public DTO2 three(Object three) {
            this.three = three;
            return this;
        }

        // Getter method for 'one' field
        public int one() {
            return one;
        }

        // Fluent setter method for 'one' field
        @NotNull
        public DTO2 one(int one) {
            this.one = one;
            return this;
        }

        // Getter method for 'two' field
        public int two() {
            return two;
        }

        // Fluent setter method for 'two' field
        @NotNull
        public DTO2 two(int two) {
            this.two = two;
            return this;
        }
    }
}
