/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("rawtypes")
public class PrimitiveTypeWrappersTest extends WireTestCommon {

    private boolean isTextWire;  // Variable to determine if using text wire format

    // Provide parameters to run the tests with
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[]{Boolean.TRUE},
                new Object[]{Boolean.TRUE}  // This seems redundant; consider having diverse values
        );
    }

    // Test writing numbers of different types and reading them back
    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @MethodSource("data")
    public void testNumbers(Object isTextWire) {
        // Define wrapper classes for numbers
        @NotNull final Class[] types = new Class[]{Byte.class,
                Short.class, Float.class,
                Integer.class, Long.class, Double.class};
        // Create an array of number instances
        @NotNull final Number[] nums = new Number[]{(byte) 1, (short) 2, (float) 3, 4, (long) 5, (double) 6};

        for (@NotNull Number num : nums) {
            for (@NotNull Class<?> type : types) {
                @NotNull final Wire wire = wireFactory();

                wire.write().object(num); // Write the number to the wire
               // System.out.println(wire);
                @Nullable final Object object = wire.read().object(type); // Read the number back as the specified type
                assertTrue(type.isAssignableFrom(object.getClass()), num.getClass() + " to " + type.getName());
                assertEquals(num.intValue(), ((Number) object).intValue(),
                        num.getClass() + " to " + type.getName());
            }
        }
    }

    // Test that writing and reading the number maintains the original type
    @ParameterizedTest
    @MethodSource("data")
    public void testNumbers2(Object isTextWire) {
        @NotNull final Number[] nums = new Number[]{(byte) 1, (short) 1, (float) 1, 1, (long) 1, (double) 1};

        for (@NotNull Number num : nums) {
            @NotNull final Wire wire = wireFactory();

            wire.write().object(num);
           // System.out.println(num.getClass() + " of " + num + " is " + (isTextWire ? wire.toString() : wire.bytes().toHexString()));
            @Nullable final Object object = wire.read().object(Object.class);
            assertSame(num.getClass(), object.getClass());
            assertEquals(num, object, num.getClass().getName());
        }
    }

    // Test writing and reading a character
    @ParameterizedTest
    @MethodSource("data")
    public void testCharacter(Object isTextWire) {
        @NotNull final Wire wire = wireFactory();
        wire.write().object('1');
        @Nullable final Object object = wire.read().object(Character.class);
        assertTrue(object instanceof Character);
        assertEquals('1', object);
    }

    // Test writing a string and reading it back as a character
    @ParameterizedTest
    @MethodSource("data")
    public void testCharacterWritenAsString(Object isTextWire) {
        @NotNull final Wire wire = wireFactory();
        wire.write().object("1");
        @Nullable final Object object = wire.read().object(Character.class);
        assertTrue(object instanceof Character);
        assertEquals('1', object);
    }

    // Test writing a character and reading it back as a string
    @ParameterizedTest
    @MethodSource("data")
    public void testCharReadAsString(Object isTextWire) {
        @NotNull final Wire wire = wireFactory();
        wire.write().object('1');
        @Nullable final Object object = wire.read().object(String.class);
        assertTrue(object instanceof String);
        assertEquals("1", object);
    }

    // Test writing a long string and reading just the first character
    @ParameterizedTest
    @MethodSource("data")
    public void testStoreStringReadAsChar(Object isTextWire) {
        @NotNull final Wire wire = wireFactory();
        wire.write().object("LONG STRING");
        @Nullable final Object object = wire.read().object(Character.class);
        assertTrue(object instanceof Character);
        assertEquals('L', object);
    }

    // Helper method to create and return a Wire instance based on the isTextWire flag
    @NotNull
    private Wire wireFactory() {
        @NotNull final Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        @NotNull Wire wire = (isTextWire) ? WireType.TEXT.apply(bytes) : new BinaryWire(bytes);

        return wire;
    }
}
