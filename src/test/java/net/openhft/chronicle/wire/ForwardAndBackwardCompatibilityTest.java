/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;

// Using the Parameterized runner for JUnit tests to enable parameter-driven tests
public class ForwardAndBackwardCompatibilityTest extends WireTestCommon {

    // Holds the WireType for this test instance
    private WireType wireType;

    // Constructor that sets the WireType
    public void initForwardAndBackwardCompatibilityTest(WireType wireType) {
        this.wireType = wireType;
    }

    // Provides the set of WireTypes to be used as parameters for the tests
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // {WireType.TEXT},
                {WireType.BINARY}
        });
    }

    // Test for checking backward compatibility of DTO classes
    @MethodSource("data")
    @ParameterizedTest
    public void backwardsCompatibility(WireType wireType) {
        initForwardAndBackwardCompatibilityTest(wireType);
        // Expecting an exception due to class replacement
        expectException("Replaced class net.openhft.chronicle.wire.ForwardAndBackwardCompatibilityTest$DTO1 with class net.openhft.chronicle.wire.ForwardAndBackwardCompatibilityTest$DTO2");

        // Creating a Wire instance based on the provided WireType
        final Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        wire.usePadding(wire.isBinary());
        CLASS_ALIASES.addAlias(DTO1.class, "DTO");

        // Writing a document with DTO1 data
        wire.writeDocument(false, w -> w.getValueOut().typedMarshallable(new DTO1(1)));

        // Switching the alias to DTO2 class
        CLASS_ALIASES.addAlias(DTO2.class, "DTO");
        if (wire instanceof TextWire)
            ((TextWire) wire).useBinaryDocuments();

        // Reading the written document and expecting to get DTO2 instance
        try (DocumentContext dc = wire.readingDocument()) {
            if (!dc.isPresent())
                Assertions.fail("document should be present when reading DTO1 as DTO2");
            @Nullable DTO2 dto2 = dc.wire().getValueIn().typedMarshallable();
            Assertions.assertEquals(1, dto2.one, "field 'one' should preserve value when reading DTO1 as DTO2");
            Assertions.assertEquals(0, dto2.two, "field 'two' should default to 0 when reading DTO1 as DTO2");
            Assertions.assertNull(dto2.three, "field 'three' should be null when reading DTO1 as DTO2");
        }

        // Releasing memory
        wire.bytes().releaseLast();
    }

    // Test for checking forward compatibility of DTO classes
    @MethodSource("data")
    @ParameterizedTest
    public void forwardCompatibility(WireType wireType) {
        initForwardAndBackwardCompatibilityTest(wireType);
        // Expecting an exception due to class replacement
        expectException("Replaced class net.openhft.chronicle.wire.ForwardAndBackwardCompatibilityTest$DTO2 with class net.openhft.chronicle.wire.ForwardAndBackwardCompatibilityTest$DTO1");

        // Creating a Wire instance based on the provided WireType
        final Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        wire.usePadding(wire.isBinary());
        CLASS_ALIASES.addAlias(DTO2.class, "DTO");

        // Writing a document with DTO2 data
        wire.writeDocument(false, w -> w.getValueOut().typedMarshallable(new DTO2(1, 2, 3)));

        // Switching the alias to DTO1 class
        CLASS_ALIASES.addAlias(DTO1.class, "DTO");
        if (wire instanceof TextWire)
            ((TextWire) wire).useBinaryDocuments();

        // Reading the written document and expecting to get DTO1 instance
        try (DocumentContext dc = wire.readingDocument()) {
            if (!dc.isPresent())
                Assertions.fail("document should be present when reading DTO2 as DTO1");
            @Nullable DTO1 dto1 = dc.wire().getValueIn().typedMarshallable();
            Assertions.assertEquals(1, dto1.one, "field 'one' should preserve value when reading DTO2 as DTO1");
        }

        // Releasing memory
        wire.bytes().releaseLast();
    }

    // Test to ensure that new data added to a document doesn't affect old reads
    @MethodSource("data")
    @ParameterizedTest
    public void testCheckThatNewDataAddedToADocumentDoesNotEffectOldReads(WireType wireType) {

        initForwardAndBackwardCompatibilityTest(wireType);

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
                Assertions.assertEquals("hello world", dc.wire().read("hello").text(), "first document should contain 'hello world'");
            }

            try (DocumentContext dc = w.readingDocument()) {
                Assertions.assertEquals("other data", dc.wire().read("other data").text(), "second document should contain 'other data'");
            }
        } finally {
            // Releasing memory
            b.releaseLast();
        }
    }

    @SuppressWarnings("this-escape")
    public static class DTO1 extends SelfDescribingMarshallable implements Demarshallable {

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
    public static class DTO2 extends SelfDescribingMarshallable implements Demarshallable {
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
