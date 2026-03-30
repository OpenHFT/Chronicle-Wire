/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import net.openhft.chronicle.core.pool.ClassLookup;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

@SuppressWarnings("this-escape")
class ForwardAndBackwardCompatibilityMarshallableTest extends WireTestCommon {

    private WireType wireType;
    private Bytes<?> bytes = Bytes.allocateElasticOnHeap();

    // Define the wire types to be tested
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {WireType.JSON},
                {WireType.TEXT},
                {WireType.YAML},
                {WireType.BINARY}
        });
    }

    // Clean up resources after tests
    @Override
    public void afterChecks() {
        bytes.releaseLast();
        super.afterChecks();
    }

    // Test to check the compatibility of a marshallable StringBuilder
    @ParameterizedTest
    @MethodSource("data")
    void marshableStringBuilderTest(WireType wireType) {
        this.wireType = wireType;
        Wire wire = wireType.apply(bytes);
        wire.usePadding(wire.isBinary());
        ClassLookup wrap1 = CLASS_ALIASES.wrap();
        wrap1.addAlias(MDTO2.class, "MDTO");
        wire.classLookup(wrap1);

        wire.writeDocument(false, w -> new MDTO2(1, 2, "3").writeMarshallable(w));
        // System.out.println(Wires.fromSizePrefixedBlobs(wire));

        try (DocumentContext dc = wire.readingDocument()) {
            if (!dc.isPresent())
                fail();
            @NotNull MDTO2 dto2 = new MDTO2();
            dto2.readMarshallable(dc.wire());
            assertEquals(1, dto2.one);
            assertEquals(2, dto2.two);
            assertTrue("3".contentEquals(dto2.three));
        }
    }

    // Test for checking backward compatibility of the Wire
    @ParameterizedTest
    @MethodSource("data")
    void backwardsCompatibility(WireType wireType) {
        this.wireType = wireType;
        Wire wire = wireType.apply(bytes);
        wire.usePadding(wire.isBinary());
        ClassLookup wrap1 = CLASS_ALIASES.wrap();
        wrap1.addAlias(MDTO2.class, "MDTO");
        wire.classLookup(wrap1);
        CLASS_ALIASES.addAlias(MDTO1.class, "MDTO");

        wire.writeDocument(false, w -> w.getValueOut().typedMarshallable(new MDTO1(1)));
        // System.out.println(Wires.fromSizePrefixedBlobs(wire));
        ClassLookup wrap2 = CLASS_ALIASES.wrap();
        wrap2.addAlias(MDTO2.class, "MDTO");
        wire.classLookup(wrap2);
        if (wire instanceof TextWire)
            ((TextWire) wire).useBinaryDocuments();
        try (DocumentContext dc = wire.readingDocument()) {
            if (!dc.isPresent())
                fail();
            @NotNull MDTO2 dto2 = new MDTO2();
            dc.wire().getValueIn().marshallable(dto2);
            assertEquals(1, dto2.one);
            assertEquals(0, dto2.two);

        }
    }

    @ParameterizedTest
    @MethodSource("data")
    void forwardCompatibility(WireType wireType) {
        this.wireType = wireType;
        // Apply the given wireType to bytes to get a Wire instance
        Wire wire = wireType.apply(bytes);

        // Check if the wire is an instance of YamlWire and skip the test if true
        assumeFalse(wire instanceof YamlWire);

        // Check if the wire is binary and apply padding if true
        wire.usePadding(wire.isBinary());

        // Wrap the CLASS_ALIASES and add an alias for MDTO2 as "MDTO"
        ClassLookup wrap2 = CLASS_ALIASES.wrap();
        wrap2.addAlias(MDTO2.class, "MDTO");
        wire.classLookup(wrap2);

        // Write a new instance of MDTO2 to the wire
        wire.writeDocument(false, w -> w.getValueOut().typedMarshallable(new MDTO2(1, 2, "3")));
        // System.out.println(Wires.fromSizePrefixedBlobs(wire));

        // Wrap the CLASS_ALIASES again and add an alias for MDTO2
        ClassLookup wrap1 = CLASS_ALIASES.wrap();
        wrap1.addAlias(MDTO2.class, "MDTO");
        wire.classLookup(wrap1);

        // If the wire is an instance of TextWire, use binary documents
        if (wire instanceof TextWire)
            ((TextWire) wire).useBinaryDocuments();

        // Read the document from the wire
        try (DocumentContext dc = wire.readingDocument()) {
            // If there's no document present, fail the test
            if (!dc.isPresent())
                fail();

            // Create a new instance of MDTO1 and read its value from the wire
            @NotNull MDTO1 dto1 = new MDTO1();
            dc.wire().getValueIn().marshallable(dto1);

            // Assert that the value read is as expected
            assertEquals(1, dto1.one);
        }
    }

    // Class representing a data transfer object with a single integer field "one"
    static class MDTO1 extends SelfDescribingMarshallable implements Demarshallable {

        int one;

        // Constructor used via reflection when reading from the wire
        @UsedViaReflection
        MDTO1(@NotNull WireIn wire) {
            readMarshallable(wire);
        }

        // Constructor to set the value of "one" directly
        MDTO1(int i) {
            this.one = i;
        }

        // Default constructor
        MDTO1() {

        }

        // Getter method for "one"
        public int one() {
            return one;
        }

        // Setter method for 'one' that returns the MDTO1 instance for chaining
        @NotNull
        public MDTO1 one(int one) {
            this.one = one;
            return this;
        }
    }

    // Class representing a data transfer object with fields "one", "two", and "three"
    static class MDTO2 extends SelfDescribingMarshallable implements Demarshallable {

        // Using StringBuilder for "three" to easily modify its content
        final StringBuilder three = new StringBuilder();
        int one;
        int two;

        // Constructor used via reflection when reading from the wire
        @UsedViaReflection
        MDTO2(@NotNull WireIn wire) {
            readMarshallable(wire);
        }

        // Constructor to initialize "one", "two", and "three" with given values
        MDTO2(int one, int two, @NotNull Object three) {
            this.one = one;
            this.two = two;
            this.three.setLength(0);
            this.three.append(three);
        }

        // Default constructor
        MDTO2() {

        }

        // Getter method for "three"
        @NotNull
        public Object three() {
            return three;
        }

        // Setter method for 'three' that returns the MDTO2 instance for chaining
        @NotNull
        public MDTO2 three(@NotNull Object three) {
            this.three.setLength(0);
            this.three.append(three);
            return this;
        }

        // Getter method for "one"
        public int one() {
            return one;
        }

        // Setter method for 'one' that returns the MDTO2 instance for chaining
        @NotNull
        public MDTO2 one(int one) {
            this.one = one;
            return this;
        }

        // Getter method for "two"
        public int two() {
            return two;
        }

        // Setter method for 'two' that returns the MDTO2 instance for chaining
        @NotNull
        public MDTO2 two(int two) {
            this.two = two;
            return this;
        }
    }
}
