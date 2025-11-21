/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.serializable;

import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.Validatable;
import net.openhft.chronicle.core.io.ValidatableUtil;
import net.openhft.chronicle.wire.FieldInfo;
import net.openhft.chronicle.wire.Wires;

import java.io.File;
import java.io.Serializable;
import java.lang.annotation.RetentionPolicy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.UUID;

import static net.openhft.chronicle.wire.WireType.TEXT;

@SuppressWarnings({"rawtypes","deprecation"})
public class ScalarValues implements Serializable, Validatable {
    private static final long serialVersionUID = 0L;

    // Path path; // commented out

    // Default constructor
    public ScalarValues() {
    }

    // Constructor that initializes fields based on an integer value
    public ScalarValues(int i) {
        // Primitive data type fields
        boolean flag = i == 0;
        byte b = (byte) i;
        short s = (short) i;
        char ch = (char) i;
        long l = i * i;
        double d = i * i;
        // Wrapper class fields for primitive types
        Boolean flag2 = !flag;
        Byte b2 = b;
        Short s2 = s;
        Character ch2 = ch;
        Integer i2 = -i;
        Float f2 = (float) i;
        Double d2 = d;
        Long l2 = l;

        // Fields of various Java standard library classes
        Class<?> aClass = net.openhft.chronicle.wire.marshallable.ScalarValues.class;
        RetentionPolicy policy = RetentionPolicy.CLASS;
        String text = "text - " + i;
        LocalDate date = LocalDate.of(i, i, i);
        LocalTime time = LocalTime.of(i, i);
        LocalDateTime dateTime = LocalDateTime.of(i, i, i, i, i, i);
        ZonedDateTime zonedDateTime = dateTime.atZone(ZoneId.of("GMT"));
        UUID uuid = new UUID(i, i);
        BigInteger bi = BigInteger.valueOf(i);
        BigDecimal bd = BigDecimal.valueOf(i);
        File file = new File("/tmp/" + i);
    }

    // Overriding equals method for custom comparison logic
    @Override
    public boolean equals(Object obj) {
        // Check for instance equality and delegate to Wires utility for deep comparison
        return obj instanceof ScalarValues && Wires.isEquals(this, obj);
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    // Overriding toString method to provide a string representation of the object
    @Override
    public String toString() {
        // Utilize TEXT Wire format for string representation
        return TEXT.asString(this);
    }

    // Implementing validate method from Validatable interface
    @Override
    public void validate() throws InvalidMarshallableException {
        // Validate all non-primitive fields to ensure they are not null
        for (FieldInfo fieldInfo : Wires.fieldInfos(getClass())) {
            if (!fieldInfo.type().isPrimitive()) {
                String name = fieldInfo.name();
                Object o = fieldInfo.get(this);
                ValidatableUtil.requireNonNull(o, name);
            }
        }
    }
}
