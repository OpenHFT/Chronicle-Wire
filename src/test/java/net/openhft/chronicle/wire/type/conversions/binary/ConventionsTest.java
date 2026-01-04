/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.type.conversions.binary;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.WireTestCommon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings("unchecked")
class ConventionsTest extends WireTestCommon {

    @Test
    @SuppressWarnings("rawtypes")
    @DisplayName("Converts max values across primitive wrapper types")
    void testTypeConversionsMaxValue() throws NoSuchFieldException, IllegalAccessException {

        for (@NotNull Class<?> type : new Class[]{String.class, Integer.class, Long.class, Short
                .class, Byte
                .class, Float.class, Double.class}) {
            Object extected;
            // Check if type is a subclass of Number
            if (Number.class.isAssignableFrom(type)) {
                // Retrieve the MAX_VALUE field from the type class
                final Field max_value = type.getField("MAX_VALUE");
                extected = max_value.get(type);
            } else {
                // For non-numeric types, use a small number as a string
                extected = "123"; // small number
            }

            // Assert equality between the expected value and the result of the test method
            Assertions.assertEquals(extected, test(extected, type),
                    "max value should round-trip for type=" + type);
        }
    }

    @Test
    @SuppressWarnings("rawtypes")
    @DisplayName("Converts min values across primitive wrapper types")
    void testTypeConversionsMinValue() throws IllegalAccessException, NoSuchFieldException {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory disabled; skip min value conversions");

        for (@NotNull Class<?> type : new Class[]{String.class, Integer.class, Long.class, Short.class, Byte
                .class, Float.class, Double.class}) {
            Object extected;
            // Check if type is a subclass of Number
            if (Number.class.isAssignableFrom(type)) {
                // Retrieve the MIN_VALUE field from the type class
                final Field value = type.getField("MIN_VALUE");
                extected = value.get(type);
            } else {
                // For non-numeric types, use a small number as a string
                extected = "123";
            }

            // Assert equality between the expected value and the result of the test method
            Assertions.assertEquals(extected, test(extected, type),
                    "min value should round-trip for type=" + type);
        }
    }

    @Test
    @SuppressWarnings("rawtypes")
    @DisplayName("Converts small numeric string values across types")
    void testTypeConversionsSmallNumber() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory disabled; skip small number conversions");

        for (@NotNull Class<?> type : new Class[]{String.class, Integer.class, Long.class, Short
                .class, Byte.class}) {
            // Use a small number as a string for the expected value
            @NotNull Object extected = "123"; // small number
            // Assert equality between the expected value and the result of the test method
            Assertions.assertEquals(extected, String.valueOf(test(extected, type)),
                    "small number should round-trip for type=" + type);
        }

        // Special cases for floating-point numbers
        Assertions.assertEquals(123.0, test("123", Double.class), 0,
                "double conversion should read 123.0");
        Assertions.assertEquals(123.0, (double) test("123", Float.class), 0,
                "float conversion should read 123.0");

    }

    @Test
    @SuppressWarnings("rawtypes")
    @DisplayName("Converts values via string for numeric types")
    void testTypeConversionsConvertViaString() throws NoSuchFieldException, IllegalAccessException {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory disabled; skip string conversion tests");

        for (@NotNull Class<?> type : new Class[]{Integer.class, Long.class, Short.class, Byte
                .class}) {
            Object extected;
            // If type is a subclass of Number, get its MAX_VALUE
            if (Number.class.isAssignableFrom(type)) {
                final Field max_value = type.getField("MAX_VALUE");
                extected = max_value.get(type);
            } else {
                // Use a default numeric value for non-numeric types
                extected = 123;
            }

            // Convert the expected value to String and then back to its original type
            @Nullable final Object value = test(extected, String.class);
            @Nullable final Object actual = test(value, extected.getClass());

            // Assert that the converted value matches the expected value
            Assertions.assertEquals(extected, actual,
                    "value should round-trip via string for type=" + type);
        }
    }

    @Test
    @DisplayName("Converts max unsigned values for long type")
    void testTypeConversionsMaxUnsigned() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory disabled; skip unsigned conversion tests");

        // Test conversions for maximum unsigned long value
        for (long shift : new long[]{8}) {
            long extected = 1L << shift;
            Assertions.assertEquals(extected, (long) test(extected, Long.class),
                    "unsigned max should round-trip for shift=" + shift);
        }
    }

    @Nullable
    private <T> T test(Object source, @NotNull Class<T> destinationType) {
        // Method to test conversion of objects to different types using Chronicle Wire
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            @NotNull final BinaryWire wire = new BinaryWire(bytes);

            // Write the source object to the wire in the appropriate format
            if (source instanceof String)
                wire.getValueOut().text((String) source);
            else if (source instanceof Long)
                wire.getValueOut().int64((Long) source);
            else if (source instanceof Integer)
                wire.getValueOut().int32((Integer) source);
            else if (source instanceof Short)
                wire.getValueOut().int16((Short) source);
            else if (source instanceof Byte)
                wire.getValueOut().int8((Byte) source);
            else if (source instanceof Float)
                wire.getValueOut().float32((Float) source);
            else if (source instanceof Double)
                wire.getValueOut().float64((Double) source);

            // Read the value from the wire and convert it to the destination type
            if (String.class.isAssignableFrom(destinationType))
                return (T) wire.getValueIn().text();

            if (Long.class.isAssignableFrom(destinationType))
                return (T) (Long) wire.getValueIn().int64();

            if (Integer.class.isAssignableFrom(destinationType))
                return (T) (Integer) wire.getValueIn().int32();

            if (Short.class.isAssignableFrom(destinationType))
                return (T) (Short) wire.getValueIn().int16();

            if (Byte.class.isAssignableFrom(destinationType))
                return (T) (Byte) wire.getValueIn().int8();

            if (Float.class.isAssignableFrom(destinationType))
                return (T) (Float) wire.getValueIn().float32();

            if (Double.class.isAssignableFrom(destinationType))
                return (T) (Double) wire.getValueIn().float64();

            // Throw an exception if the conversion is not supported
            throw new UnsupportedOperationException("Unsupported destination type " + destinationType);
        } finally {
            // Release resources associated with the Bytes object
            bytes.releaseLast();
        }
    }
}
