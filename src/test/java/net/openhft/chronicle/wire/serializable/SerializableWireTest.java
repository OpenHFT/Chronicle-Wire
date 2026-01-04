/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.serializable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class SerializableWireTest extends WireTestCommon {
    // Parameterized tests with various combinations of wire types and serializable objects
    @NotNull // toString() implicility called here
    public static Collection<Object[]> combinations() {
        @NotNull List<Object[]> list = new ArrayList<>();
        // Wire types for testing
        @NotNull WireType[] wireTypes = {WireType.TEXT /*, WireType.YAML_ONLY, WireType.BINARY*/};
        // Serializable objects for testing
        @NotNull Serializable[] objects = {
            // Various serializable objects to test
            new Nested(),
            new SerializableScalarValues(),
            new Nested(new SerializableScalarValues(), Collections.emptyList(), Collections.emptySet(), Collections.emptyMap()),
            new Nested(new SerializableScalarValues(1), null, Collections.emptySet(), Collections.emptyMap()),
            new Nested(new SerializableScalarValues(1), Collections.emptyList(), Collections.emptySet(), Collections.emptyMap()),
            new SerializableScalarValues(1),
            new SerializableScalarValues(10)
        };
        // Generate combinations of wire types and serializable objects
        for (WireType wt : wireTypes) {
            for (Serializable object : objects) {
                @NotNull Object[] test = {wt, object, list.size() < 4};
                list.add(test);
            }
        }
        return list;
    }

    // Test method to write and read serializable objects using different wire types
    @MethodSource("combinations")
    @SuppressWarnings("rawtypes")
    @ParameterizedTest(name = "wire round-trip: wt={0}, object={1}, IME={2}")
    @DisplayName("Serialisable objects round-trip via wire")
    public void writeMarshallable(WireType wireType, Serializable m, boolean ime) {
        // Ignore exceptions for certain test cases
        if (ime) // TODO Fix to be expected
            ignoreException(ek -> ek.throwable instanceof InvalidMarshallableException, "IME");
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            // Apply wire type to bytes
            Wire wire = wireType.apply(bytes);

            // Write the serializable object to wire
            wire.getValueOut().object(m);

            // Read the object back from wire
            @Nullable Object m2 = wire.getValueIn().object();
            // Assert that the written and read objects are equal
            assertEquals(m, m2, "Serializable object should round-trip for " + wireType);
            // Fail if an exception was expected but not thrown
            if (ime)
                fail("InvalidMarshallableException should have been thrown");
        } catch (InvalidMarshallableException e) {
            // Throw exception if it was not expected
            if (!ime)
                throw e;
        } finally {
            // Release bytes
            bytes.releaseLast();
        }
    }

    @MethodSource("combinations")
    @ParameterizedTest(name = "string builder round-trip: wt={0}, object={1}, IME={2}")
    @DisplayName("StringBuilder serialises inside marshallable containers correctly")
    public void testStringBuilderSerialization(WireType wireType, Serializable m, boolean ime) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            Wire wire = new BinaryWire(bytes);
            TextContainer outerContainer = new TextContainer();
            outerContainer.innerBuilders = new StringBuilder[]{new StringBuilder("innerText")};
            wire.write("data").object(outerContainer);

            TextContainer deserializedContainer = wire.read("data").object(TextContainer.class);

            assertEquals(outerContainer.innerBuilders[0].toString(), deserializedContainer.innerBuilders[0].toString(),
                    "StringBuilder content should round-trip");
        } finally {
            bytes.releaseLast();
        }
    }

    static class TextContainer extends SelfDescribingMarshallable {
        StringBuilder[] innerBuilders; // Represents inner StringBuilders
    }
}
