/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.reordered;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * Test class extending WireTestCommon to validate serialization and deserialization behaviors
 * using different wire types in Chronicle Wire. This class uses parameterized tests to
 * execute the same set of tests with various wire formats.
 */
class ReorderedTest extends WireTestCommon {
    // Static instances of OuterClass for test setup
    private static final OuterClass outerClass1 = new OuterClass();
    private static final OuterClass outerClass2 = new OuterClass();

    // Static initialization block to configure the OuterClass instances for testing
    static {
        // Setting texts and wire types for the instances
        outerClass1.setText("text1");
        outerClass2.setText("text2");
        outerClass1.setWireType(WireType.BINARY);
        outerClass2.setWireType(WireType.TEXT);

        // Clearing and populating lists within the OuterClass instances
        outerClass1.clearListA();
        outerClass2.clearListA();
        outerClass1.clearListB();
        outerClass2.clearListB();
        // Adding specific nested items to the lists
        outerClass1.addListA().setTextNumber("num1A", 11);
        outerClass1.addListB().setTextNumber("num1B", 12).nest("num1Bbis", 121);
        outerClass1.addListA().setTextNumber("num1AA", 111).nest("num1AAbis", 1111);
        outerClass1.addListB().setTextNumber("num1BB", 122);
        outerClass2.addListA().setTextNumber("num2A", 21);
        outerClass2.addListB().setTextNumber("num2B", 22).nest("num2Bbis", 222);

        // Initializing a collection of NestedReadSubsets for testing
        nestedReadSubsets = Arrays.asList(
                new NestedReadSubset().setTextNumber("one", 1.1),
                new NestedReadSubset().setTextNumber("two", 2.2));
    }

    private static final Collection<NestedReadSubset> nestedReadSubsets;

    // Constructor accepting the wire type function
    @SuppressWarnings("rawtypes")
    // Parameterized test configurations
    public static Collection<Function<Bytes<?>, Wire>> combinations() {
        return Arrays.asList(
                WireType.JSON,
                WireType.TEXT,
                // https://github.com/OpenHFT/Chronicle-Wire/issues/665
                //{WireType.YAML_ONLY},
                WireType.BINARY
        );
    }

    /**
     * Test to verify that fields can be reordered during serialization and deserialization.
     * It writes and then reads back `OuterClass` objects to ensure the data remains consistent
     * across these operations.
     */
    @SuppressWarnings("rawtypes")
    @ParameterizedTest
    @MethodSource("combinations")
    void testWithReorderedFields(Function<Bytes<?>, Wire> wireType) {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Bytes<?> bytes = Bytes.elasticByteBuffer();
        Wire wire = wireType.apply(bytes);

        // Writing two instances of OuterClass with event names
        wire.writeEventName(() -> "test1").marshallable(outerClass1);
        // Adding a newline for JSON wire type
        if (wireType == WireType.JSON)
            wire.bytes().writeUnsignedByte('\n');
        wire.writeEventName(() -> "test2").marshallable(outerClass2);

        // System.out.println(bytes.readByte(0) < 0 ? bytes.toHexString() : bytes.toString());
        @NotNull StringBuilder sb = new StringBuilder();
        @NotNull OuterClass outerClass0 = new OuterClass();

        // Reading back the first OuterClass instance and comparing
        wire.readEventName(sb).marshallable(outerClass0);
        assertEquals("test1", sb.toString());
        assertEquals(outerClass1.toString().replace(',', '\n'), outerClass0.toString().replace(',', '\n'));

        // Reading back the second OuterClass instance and comparing
        wire.readEventName(sb).marshallable(outerClass0);
        assertEquals("test2", sb.toString());
        assertEquals(outerClass2.toString().replace(',', '\n'), outerClass0.toString().replace(',', '\n'));

        bytes.releaseLast();
    }

    /**
     * Test to verify serialization and deserialization of a collection of objects.
     * It writes a collection of `NestedReadSubset` objects and then reads them back.
     */
    @ParameterizedTest
    @MethodSource("combinations")
    void testWithSubsetFields(Function<Bytes<?>, Wire> wireType) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = wireType.apply(bytes);

        // Writing a collection of NestedReadSubset objects
        wire.writeEventName(() -> "test1").collection(nestedReadSubsets, NestedReadSubset.class);

        @NotNull StringBuilder sb = new StringBuilder();

        // Reading the collection back and comparing
        assertEquals(nestedReadSubsets.toString().replace(',', '\n'), wire.readEventName(sb).collection(ArrayList::new, NestedReadSubset.class).toString().replace(',', '\n'));
        assertEquals("test1", sb.toString());
    }

    /**
     * Test to verify reading and writing of top-level fields directly on a wire,
     * not nested within a marshallable object. This test runs a loop to perform
     * multiple iterations with different values.
     */
    @SuppressWarnings("rawtypes")
    @ParameterizedTest
    @MethodSource("combinations")
    void testTopLevel(Function<Bytes<?>, Wire> wireType) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = wireType.apply(bytes);
        for (int i = 1; i < 5; i++) {
            wire.clear();
            // Writing three fields with values dependent on the loop variable
            wire.write("a").int32(i);
            wire.write("b").int32(i * 11);
            wire.write("c").int32(i * 111);

            // System.out.println(wire);
            // Reading back the fields in a different order and asserting
            assertEquals(i * 111, wire.read(() -> "c").int32());
            assertEquals(i, wire.read(() -> "a").int32());
            assertEquals(i * 11, wire.read(() -> "b").int32());
        }

        bytes.releaseLast();
    }
}
