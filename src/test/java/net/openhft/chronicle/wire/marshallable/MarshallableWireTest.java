/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

// Runner to enable parameterized tests for the MarshallableWireTest class
public class MarshallableWireTest extends WireTestCommon {

    // Type of wire to be tested
    private WireType wireType;

    // Marshallable object for test scenarios
    private Marshallable m;

    // Constructor initializes the WireType and Marshallable object for the test scenario
    public void initMarshallableWireTest(WireType wireType, Marshallable m) {
        this.wireType = wireType;
        this.m = m;
    }

    @BeforeEach
    public void hasDirect() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
    }

    // Provide test data combinations for the parameterized test
    @NotNull
    public static Collection<Object[]> combinations() {

        // Collection to store different test combinations
        @NotNull List<Object[]> list = new ArrayList<>();

        // Different WireTypes for test scenarios
        @NotNull WireType[] wireTypes = {WireType.TEXT, WireType.YAML_ONLY, WireType.BINARY};

        // Sample Marshallable objects for the test scenarios
        @NotNull Marshallable[] objects = {
                new Nested(),
                new Nested(new ScalarValues(), Collections.emptyList(), Collections.emptySet(), Collections.emptyMap(), new String[0]),
                new ScalarValues(),
                new ScalarValues(1),
                new ScalarValues(10)
        };

        // Populate the test combinations list using each WireType with each Marshallable object
        for (WireType wt : wireTypes) {
            for (Marshallable object : objects) {
                @NotNull Object[] test = {wt, object};
                list.add(test);
            }
        }

        return list;
    }

    // Test method to write a Marshallable object to a wire and then read it back
    @MethodSource("combinations")
    @SuppressWarnings("rawtypes")
    @ParameterizedTest
    public void writeMarshallable(WireType wireType, Marshallable m) {

        initMarshallableWireTest(wireType, m);

        // Allocate memory for writing data
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();

        // Apply the specific WireType to the allocated memory
        Wire wire = wireType.apply(bytes);

        // Write the Marshallable object to the wire
        wire.getValueOut().object(m);

        // Uncomment to print wire contents for debug purposes

        // Read back the object from the wire
        @Nullable Object m2 = wire.getValueIn().object();

        // Assert that the written and read objects are the same
        if (!m.equals(m2))
            assertEquals(m, m2);

        // Release the allocated memory
        bytes.releaseLast();
    }
}
