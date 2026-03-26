/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class Issue341Test extends WireTestCommon {

    // Instance variable to store the current WireType that the test is running for.
    private WireType wireType;

    // This method specifies the different WireTypes the tests will run for.
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                new Object[]{WireType.BINARY},
                new Object[]{WireType.BINARY_LIGHT},
                new Object[]{WireType.JSON},
                new Object[]{WireType.JSON_ONLY},
                new Object[]{WireType.TEXT},
                new Object[]{WireType.YAML},
                new Object[]{WireType.YAML_ONLY},
        });
    }

    // Test for serializing and deserializing an instance of MyClass using different WireTypes.
    @ParameterizedTest
    @MethodSource("data")
    void instant(WireType wireType) {
        this.wireType = wireType;
        final MyClass source = new MyClass();
        source.instant = Instant.ofEpochMilli(1_000_000_000_000L);

        // Create bytes from HexDumpBytes for serialization.
        final Bytes<?> bytes = new HexDumpBytes();
        // Create a wire instance based on the current WireType.
        Wire wire = wireType.apply(bytes);

        wire.getValueOut().object(Jvm.uncheckedCast(source.getClass()), source);
        System.out.println(wireType + "\n"
                + (wire.getValueOut().isBinary() ? bytes.toHexString() : bytes.toString()));

        // Deserialize the source object from the wire.
        final MyClass target = wire.getValueIn().object(source.getClass());

        // Verify that the deserialized object matches the original source object.
        assertEquals(source, target);

    }

    // Test for serializing and deserializing an instance of MyComparableSerializable using different WireTypes.
    @ParameterizedTest
    @MethodSource("data")
    void testComparableSerializable(WireType wireType) {
        this.wireType = wireType;
        // for backward compatibility, this doesn't support types
        assumeFalse(wireType == WireType.JSON);
        final MyComparableSerializable source = new MyComparableSerializable("hello");

        // Create bytes from HexDumpBytes for serialization.
        final Bytes<?> bytes = new HexDumpBytes();
        // Create a wire instance based on the current WireType.
        Wire wire = wireType.apply(bytes);

        wire.getValueOut().object(Jvm.uncheckedCast(source.getClass()), source);
        System.out.println(wireType + "\n"
                + (wire.getValueOut().isBinary() ? bytes.toHexString() : bytes.toString()));

        // Deserialize the source object from the wire.
        final MyComparableSerializable target = wire.getValueIn().object(source.getClass());

        // Verify that the deserialized object's value matches the original source object's value.
        assertEquals(source.value, target.value);
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
    }
}
