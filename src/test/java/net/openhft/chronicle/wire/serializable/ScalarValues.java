/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
    // Primitive data type fields
    public boolean flag;
    public byte b;
    public short s;
    public char ch;
    public int i;
    public float f;
    public long l;
    public double d;

    // Wrapper class fields for primitive types
    public Boolean flag2;
    public Byte b2;
    public Short s2;
    public Character ch2;
    public Integer i2;
    public Float f2;
    public Long l2;
    public Double d2;

    // Fields of various Java standard library classes
    public Class<?> aClass;
    public RetentionPolicy policy;
    public String text;
    public LocalDate date;
    public LocalTime time;
    public LocalDateTime dateTime;
    public ZonedDateTime zonedDateTime;
    public UUID uuid;
    public BigInteger bi;
    public BigDecimal bd;
    public File file;
    // Path path; // commented out

    // Default constructor
    public ScalarValues() {
    }

    // Constructor that initializes fields based on an integer value
    public ScalarValues(int i) {
        flag = i == 0;
        b = (byte) i;
        s = (short) i;
        ch = (char) i;
        this.i = i;
        f = i;
        l = i * i;
        d = i * i;
        flag2 = !flag;
        b2 = b;
        s2 = s;
        ch2 = ch;
        i2 = -i;
        f2 = f;
        d2 = d;
        l2 = l;

        aClass = net.openhft.chronicle.wire.marshallable.ScalarValues.class;
        policy = RetentionPolicy.CLASS;
        text = "text - " + i;
        date = LocalDate.of(i, i, i);
        time = LocalTime.of(i, i);
        dateTime = LocalDateTime.of(i, i, i, i, i, i);
        zonedDateTime = dateTime.atZone(ZoneId.of("GMT"));
        uuid = new UUID(i, i);
        bi = BigInteger.valueOf(i);
        bd = BigDecimal.valueOf(i);
        file = new File("/tmp/" + i);
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
