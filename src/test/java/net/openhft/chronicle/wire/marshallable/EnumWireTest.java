/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.ReadResolvable;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

/**
 * A suite of tests focused on checking the serialization/deserialization
 * behavior of different types of enums using various wire formats.
 *
 * @author greg allen
 */
class EnumWireTest extends WireTestCommon {

    // Parameterized test setup: Returns a list of wire creation strategies to be used for the test iterations.
    public static Iterable<Function<Bytes<?>, Wire>> wires() {
        return Arrays.asList(YamlWire::new, TextWire::new, BinaryWire::new, RawWire::new);
    }

    // Helper method that serializes a given marshallable object (like Person) using the provided wire strategy.
    private static Wire serialise(@NotNull Function<Bytes<?>, Wire> createWire, @NotNull Marshallable person) {
        Wire wire = createWire.apply(Bytes.allocateElasticOnHeap());
        person.writeMarshallable(wire);
        return wire;
    }

    // Test case that checks the correct deserialization of an enum that implements Marshallable.
    @ParameterizedTest
    @MethodSource("wires")
    void testEnumImplementingMarshallable(Function<Bytes<?>, Wire> createWire) {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        assertSame(Marsh.MARSH, roundTrip(createWire, Person1::new).field);
    }

    // Test case that checks the correct deserialization of an enum that does NOT implement Marshallable.
    @ParameterizedTest
    @MethodSource("wires")
    void testEnumNotImplementingMarshallable(Function<Bytes<?>, Wire> createWire) {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        assertSame(NoMarsh.NO_MARSH, roundTrip(createWire, Person2::new).field);
    }

    // Test case that checks the correct deserialization of an object that's intended to behave like an enum,
    // and implements both Marshallable and ReadResolvable.
    @ParameterizedTest
    @MethodSource("wires")
    void testEnumImplementingMarshallableAndReadResolve(Function<Bytes<?>, Wire> createWire) {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        assertSame(MarshAndResolve.MARSH_AND_RESOLVE, roundTrip(createWire, Person3::new).field);
    }

    // Helper method that serializes an object using the current wire strategy and then deserializes it.
    private <T extends Marshallable> T roundTrip(Function<Bytes<?>, Wire> createWire, @NotNull Supplier<T> supplier) {
        Wire wire = serialise(createWire, supplier.get());
        // System.out.println(wire.bytes());
        try {
            T deserialized = supplier.get();
            deserialized.readMarshallable(wire);
            return deserialized;
        } finally {
            wire.bytes().releaseLast();
        }
    }

    // Enum type that implements Marshallable.
    enum Marsh implements Marshallable {
        MARSH
    }

    // Enum type that doesn't implement any additional interfaces.
    enum NoMarsh {
        NO_MARSH
    }

    // Class intended to behave like an enum; implements both Marshallable and ReadResolvable.
    static class MarshAndResolve implements Marshallable, ReadResolvable<MarshAndResolve> {
        static final MarshAndResolve MARSH_AND_RESOLVE = new MarshAndResolve();

        @Override
        @NotNull
        public MarshAndResolve readResolve() {
            return MARSH_AND_RESOLVE;
        }
    }

    // DTO with a field of type Marsh.
    static class Person1 extends SelfDescribingMarshallable {
        @NotNull
        private Marsh field = Marsh.MARSH;
    }

    // DTO with a field of type NoMarsh.
    static class Person2 extends SelfDescribingMarshallable {
        @NotNull
        private NoMarsh field = NoMarsh.NO_MARSH;
    }

    // DTO with a field of type MarshAndResolve.
    static class Person3 extends SelfDescribingMarshallable {
        @NotNull
        private MarshAndResolve field = MarshAndResolve.MARSH_AND_RESOLVE;
    }
}
