/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.reordered;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import net.openhft.chronicle.wire.reuse.OuterClassWireTestSupport;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Test class extending WireTestCommon to validate serialization and deserialization behaviors
 * using different wire types in Chronicle Wire. This class uses parameterized tests to
 * execute the same set of tests with various wire formats.
 */
class ReorderedTest extends WireTestCommon {
    // Static instances of OuterClass for test setup
    private static final OuterClass outerClass1 = new OuterClass();
    private static final OuterClass outerClass2 = new OuterClass();
    private static final Collection<NestedReadSubset> nestedReadSubsets;

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

    // Parameterized test configurations
    public static Collection<Object[]> combinations() {
        return Arrays.asList(new Object[][]{
                {WireType.JSON},
                {WireType.TEXT},
                // https://github.com/OpenHFT/Chronicle-Wire/issues/665
                //{WireType.YAML_ONLY},
                {WireType.BINARY}
        });
    }

    /**
     * Test to verify that fields can be reordered during serialization and deserialization.
     * It writes and then reads back `OuterClass` objects to ensure the data remains consistent
     * across these operations.
     */
    @MethodSource("combinations")
    @SuppressWarnings("rawtypes")
    @ParameterizedTest(name = "{0}")
    @DisplayName("Reordered field reads remain consistent across wire types")
    void testWithReorderedFields(Function<Bytes<?>, Wire> wireType) {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for reordered field tests");

        OuterClassWireTestSupport.assertTwoOuterClasses(wireType, OuterClass::new, outerClass1, outerClass2,
                true /* helper asserts round-trip invariants */);
    }

    /**
     * Test to verify serialization and deserialization of a collection of objects.
     * It writes a collection of `NestedReadSubset` objects and then reads them back.
     */
    @MethodSource("combinations")
    @ParameterizedTest(name = "{0}")
    @DisplayName("Subset collections round-trip in wire output")
    void testWithSubsetFields(Function<Bytes<?>, Wire> wireType) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = wireType.apply(bytes);

        // Writing a collection of NestedReadSubset objects
        wire.writeEventName(() -> "test1").collection(nestedReadSubsets, NestedReadSubset.class);

        @NotNull StringBuilder sb = new StringBuilder();

        // Reading the collection back and comparing
        assertEquals(nestedReadSubsets.toString().replace(',', '\n'),
                wire.readEventName(sb).collection(ArrayList::new, NestedReadSubset.class).toString().replace(',', '\n'),
                "Nested subsets should round-trip via collection read");
        assertEquals("test1", sb.toString(), "Event name should be test1");
    }

    /**
     * Test to verify reading and writing of top-level fields directly on a wire,
     * not nested within a marshallable object. This test runs a loop to perform
     * multiple iterations with different values.
     */
    @MethodSource("combinations")
    @SuppressWarnings("rawtypes")
    @ParameterizedTest(name = "{0}")
    @DisplayName("Top-level fields read in reordered sequence")
    void testTopLevel(Function<Bytes<?>, Wire> wireType) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = wireType.apply(bytes);
        for (int i = 1; i < 5; i++) {
            wire.clear();
            // Writing three fields with values dependent on the loop variable
            wire.write("a").int32(i);
            wire.write("b").int32(i * 11);
            wire.write("c").int32(i * 111);

            // Reading back the fields in a different order and asserting
            assertEquals(i * 111, wire.read(() -> "c").int32(), "Field c should match at iteration " + i);
            assertEquals(i, wire.read(() -> "a").int32(), "Field a should match at iteration " + i);
            assertEquals(i * 11, wire.read(() -> "b").int32(), "Field b should match at iteration " + i);
        }

        bytes.releaseLast();
    }
}
