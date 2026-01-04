/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;

@SuppressWarnings("rawtypes")
class PrimitiveTypeWrappersTest extends WireTestCommon {

    private boolean isTextWire;  // Variable to determine if using text wire format

    // Constructor to initialize the isTextWire flag
    void initPrimitiveTypeWrappersTest(Object isTextWire) {
        this.isTextWire = (Boolean) isTextWire;
    }

    // Provide parameters to run the tests with
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[]{Boolean.TRUE},
                new Object[]{Boolean.TRUE}  // This seems redundant; consider having diverse values
        );
    }

    // Test writing numbers of different types and reading them back
    @MethodSource("data")
    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @DisplayName("Numbers round-trip across wrapper types")
    void testNumbers(Object isTextWire) {
        initPrimitiveTypeWrappersTest(isTextWire);
        // Define wrapper classes for numbers
        @NotNull final Class[] types = {Byte.class,
                Short.class, Float.class,
                Integer.class, Long.class, Double.class};
        // Create an array of number instances
        @NotNull final Number[] nums = {(byte) 1, (short) 2, (float) 3, 4, (long) 5, (double) 6};

        for (@NotNull Number num : nums) {
            for (@NotNull Class<?> type : types) {
                @NotNull final Wire wire = wireFactory();

                wire.write().object(num); // Write the number to the wire
                @Nullable final Object object = wire.read().object(type); // Read the number back as the specified type
                Assertions.assertTrue(type.isAssignableFrom(object.getClass()),
                        "Read type should be assignable for " + num.getClass().getName() + " to " + type.getName());
                Assertions.assertEquals(num.intValue(), ((Number) object).intValue(),
                        "Numeric value should round-trip for " + num.getClass().getName() + " to " + type.getName());
            }
        }
    }

    // Test that writing and reading the number maintains the original type
    @MethodSource("data")
    @ParameterizedTest
    @DisplayName("Numbers retain original wrapper type on read")
    void testNumbers2(Object isTextWire) {
        initPrimitiveTypeWrappersTest(isTextWire);
        @NotNull final Number[] nums = {(byte) 1, (short) 1, (float) 1, 1, (long) 1, (double) 1};

        for (@NotNull Number num : nums) {
            @NotNull final Wire wire = wireFactory();

            wire.write().object(num);
            @Nullable final Object object = wire.read().object(Object.class);
            Assertions.assertSame(num.getClass(), object.getClass(),
                    "Wire should preserve the wrapper type for " + num.getClass().getName());
            Assertions.assertEquals(num, object, "Number should round-trip for " + num.getClass().getName());
        }
    }

    // Test writing and reading a character
    @MethodSource("data")
    @ParameterizedTest
    @DisplayName("Character round-trips as Character type")
    void testCharacter(Object isTextWire) {
        initPrimitiveTypeWrappersTest(isTextWire);
        @NotNull final Wire wire = wireFactory();
        wire.write().object('1');
        @Nullable final Object object = wire.read().object(Character.class);
        Assertions.assertInstanceOf(Character.class, object,
                "Read value should be a Character wrapper for input");
        Assertions.assertEquals('1', object, "Character should round-trip when read as Character");
    }

    // Test writing a string and reading it back as a character
    @MethodSource("data")
    @ParameterizedTest
    @DisplayName("String written reads back as Character")
    void testCharacterWritenAsString(Object isTextWire) {
        initPrimitiveTypeWrappersTest(isTextWire);
        @NotNull final Wire wire = wireFactory();
        wire.write().object("1");
        @Nullable final Object object = wire.read().object(Character.class);
        Assertions.assertInstanceOf(Character.class, object, "String read as Character should yield a Character");
        Assertions.assertEquals('1', object, "String \"1\" should read as character '1'");
    }

    // Test writing a character and reading it back as a string
    @MethodSource("data")
    @ParameterizedTest
    @DisplayName("Character written reads back as String")
    void testCharReadAsString(Object isTextWire) {
        initPrimitiveTypeWrappersTest(isTextWire);
        @NotNull final Wire wire = wireFactory();
        wire.write().object('1');
        @Nullable final Object object = wire.read().object(String.class);
        Assertions.assertInstanceOf(String.class, object, "Character read as String should yield a String");
        Assertions.assertEquals("1", object, "Character '1' should read back as \"1\"");
    }

    // Test writing a long string and reading just the first character
    @MethodSource("data")
    @ParameterizedTest
    @DisplayName("Long string reads first character only")
    void testStoreStringReadAsChar(Object isTextWire) {
        initPrimitiveTypeWrappersTest(isTextWire);
        @NotNull final Wire wire = wireFactory();
        wire.write().object("LONG STRING");
        @Nullable final Object object = wire.read().object(Character.class);
        Assertions.assertInstanceOf(Character.class, object, "Long string read as Character should yield a Character");
        Assertions.assertEquals('L', object, "Character should be the first letter of the string");
    }

    // Helper method to create and return a Wire instance based on the isTextWire flag
    @NotNull
    private Wire wireFactory() {
        @NotNull final Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        @NotNull Wire wire = (isTextWire) ? WireType.TEXT.apply(bytes) : new BinaryWire(bytes);

        return wire;
    }
}
