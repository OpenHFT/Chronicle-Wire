/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

class Issue341Test extends WireTestCommon {

    // This method specifies the different WireTypes the tests will run for.
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[]{WireType.BINARY},
                new Object[]{WireType.BINARY_LIGHT},
                new Object[]{WireType.JSON},
                new Object[]{WireType.JSON_ONLY},
                new Object[]{WireType.TEXT},
                new Object[]{WireType.YAML},
                new Object[]{WireType.YAML_ONLY});
    }

    // Test for serializing and deserializing an instance of MyClass using different WireTypes.
    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    @DisplayName("Serialises Instant field across wire types")
    void instant(WireType wireType) {
        final MyClass source = new MyClass();
        source.instant = Instant.ofEpochMilli(1_000_000_000_000L);

        // Create bytes from HexDumpBytes for serialization.
        final Bytes<?> bytes = new HexDumpBytes();
        // Create a wire instance based on the current WireType.
        final Wire wire = wireType.apply(bytes);

        wire.getValueOut().object(Jvm.uncheckedCast(source.getClass()), source);
        System.out.println(wireType + "\n"
                + (wire.getValueOut().isBinary() ? bytes.toHexString() : bytes.toString()));

        // Deserialize the source object from the wire.
        final MyClass target = wire.getValueIn().object(source.getClass());

        // Verify that the deserialized object matches the original source object.
        Assertions.assertEquals(source, target,
                "Instant field should round-trip for wireType=" + wireType);
        Assertions.assertEquals(source.instant, target.instant,
                "Instant value should round-trip for wireType=" + wireType);

    }

    // Test for serializing and deserializing an instance of MyComparableSerializable using different WireTypes.
    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    @DisplayName("Serialises comparable serialisable across wire types")
    void testComparableSerializable(WireType wireType) {
        // for backward compatibility, this doesn't support types
        assumeFalse(wireType == WireType.JSON,
                "json wire does not support types for Issue341, wireType=" + wireType);
        final MyComparableSerializable source = new MyComparableSerializable("hello");

        // Create bytes from HexDumpBytes for serialization.
        final Bytes<?> bytes = new HexDumpBytes();
        // Create a wire instance based on the current WireType.
        final Wire wire = wireType.apply(bytes);

        wire.getValueOut().object(Jvm.uncheckedCast(source.getClass()), source);
        System.out.println(wireType + "\n"
                + (wire.getValueOut().isBinary() ? bytes.toHexString() : bytes.toString()));

        // Deserialize the source object from the wire.
        final MyComparableSerializable target = wire.getValueIn().object(source.getClass());

        // Verify that the deserialized object's value matches the original source object's value.
        Assertions.assertEquals(source.value, target.value,
                "comparable serialisable value should round-trip for wireType=" + wireType);
    }

    // Class that represents a test object with an Instant property.
    static final class MyClass extends SelfDescribingMarshallable {
        Instant instant;
    }

    // Class that represents a test object with a String value and implements Serializable and Comparable.
    static final class MyComparableSerializable implements Serializable, Comparable<MyComparableSerializable> {
        private static final long serialVersionUID = 0L;
        final String value;

        // Constructor to initialize the object with the given value.
        MyComparableSerializable(String value) {
            this.value = value;
        }

        // Return the string representation of this object.
        @Override
        public String toString() {
            return value;
        }

        // Compare this object with another MyComparableSerializable object based on their values.
        @Override
        public int compareTo(@NotNull MyComparableSerializable o) {
            return value.compareTo(o.value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof MyComparableSerializable))
                return false;
            MyComparableSerializable that = (MyComparableSerializable) o;
            return value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }
}
