/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AbstractUntypedFieldTest extends WireTestCommon {

    // Provide different wire format factories (JSON, Text, and YAML) for parameterized tests
    private static Stream<Function<Bytes<byte[]>, Wire>> provideWire() {
        return Stream.of(
                JSONWire::new,
                TextWire::new,
                YamlWire::new
        );
    }

    // Add an alias for AImpl before each test
    @BeforeEach
    void beforeEach() {
        ClassAliasPool.CLASS_ALIASES.addAlias(AImpl.class, "AImpl");
    }

    // Parameterized test to verify typed fields are not null
    @ParameterizedTest
    @MethodSource("provideWire")
    @DisplayName("Typed fields deserialise as non-null values")
    void typedFieldsShouldBeNonNull(Function<Bytes<byte[]>, Wire> wireConstruction) {
        final Bytes<byte[]> bytes = Bytes.from("!net.openhft.chronicle.wire.AbstractUntypedFieldTest$Holder {\n" +
                "  a: !AImpl {\n" +
                "  }\n" +
                "}");
        final Wire textWire = wireConstruction.apply(bytes);
        final String wireName = textWire.getClass().getSimpleName();

        final Holder holder = textWire.getValueIn().object(Holder.class);

        System.out.println("holder.a = " + holder.a);

        Holder expected = new Holder(new AImpl());
        assertNotNull(expected.a, "Sample holder should set a field");

        // Assertion to check if the typed field is not null
        assertNotNull(holder.a, "Typed field should be non-null for wire=" + wireName);
    }

    // Parameterized test to verify untyped fields are null
    @ParameterizedTest
    @MethodSource("provideWire")
    @DisplayName("Untyped fields deserialise as null values")
    void untypedFieldsShouldBeNull(Function<Bytes<byte[]>, Wire> wireConstruction) {
        final Bytes<byte[]> bytes = Bytes.from("!net.openhft.chronicle.wire.AbstractUntypedFieldTest$Holder {\n" +
                "  a: {\n" +
                "  }\n" +
                "}");
        final Wire textWire = wireConstruction.apply(bytes);
        final String wireName = textWire.getClass().getSimpleName();

        ignoreException("Ignoring exception and setting field 'a' to null");
        final Holder holder = textWire.getValueIn().object(Holder.class);

        // Assertion to check if the untyped field is null
        assertNull(holder.a, "Untyped field should be null for wire=" + wireName);
    }

    // Parameterized test to ensure that missing aliases result in warnings
    @ParameterizedTest
    @MethodSource("provideWire")
    @DisplayName("Missing aliases log warnings and set null fields")
    void missingAliasesShouldLogWarnings(Function<Bytes<byte[]>, Wire> wireConstruction) {
        final Bytes<byte[]> bytes = Bytes.from("!net.openhft.chronicle.wire.AbstractUntypedFieldTest$Holder {\n" +
                "  a: !MissingAlias {\n" +
                "  }\n" +
                "}");
        final Wire textWire = wireConstruction.apply(bytes);
        final String wireName = textWire.getClass().getSimpleName();

        // Expect certain exception messages to be logged
        expectException("Ignoring exception and setting field 'a' to null");
        ignoreException("MissingAlias");
        final ValueIn valueIn = textWire.getValueIn();

        // Assertion to check if the field with missing alias is null
        assertNull(valueIn.object(Holder.class).a,
                "Missing alias field should be null for wire=" + wireName);
    }

    // Abstract base class for testing
    abstract static class A {
    }

    // Implementation of the abstract base class
    private static final class AImpl extends A {
    }

    // Holder class to hold instances of type A
    static final class Holder {
        A a;

        Holder() {
        }

        Holder(A a) {
            this.a = a;
        }
    }
}
