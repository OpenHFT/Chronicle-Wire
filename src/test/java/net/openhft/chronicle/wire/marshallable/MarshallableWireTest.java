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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

// Runner to enable parameterized tests for the MarshallableWireTest class
class MarshallableWireTest extends WireTestCommon {

    @BeforeEach
    void hasDirect() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory must be available for Marshallable wire tests");
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
    @DisplayName("Selected wire should round-trip Marshallable instances")
    void writeMarshallable(WireType wireType, Marshallable m) {
        // Allocate memory for writing data
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();

        // Apply the specific WireType to the allocated memory
        Wire wire = wireType.apply(bytes);

        // Write the Marshallable object to the wire
        wire.getValueOut().object(m);

        // Uncomment to print wire contents for debug purposes

        // Read back the object from the wire
        @Nullable Object m2 = wire.getValueIn().object();

        assertNotNull(m2, "Wire round-trip should return non-null object for " + m.getClass().getSimpleName());

        if (m instanceof Nested && m2 instanceof Nested) {
            assertEquals(((Nested) m).fieldFingerprint(), ((Nested) m2).fieldFingerprint(),
                    "Nested field fingerprint should round-trip for wireType=" + wireType);
        }
        if (m instanceof ScalarValues && m2 instanceof ScalarValues) {
            assertEquals(((ScalarValues) m).fieldFingerprint(), ((ScalarValues) m2).fieldFingerprint(),
                    "ScalarValues field fingerprint should round-trip for wireType=" + wireType);
        }

        // Assert that the written and read objects are the same
        if (!m.equals(m2))
            assertEquals(m, m2, "Marshallable should round-trip via selected wire type");

        // Release the allocated memory
        bytes.releaseLast();
    }
}
