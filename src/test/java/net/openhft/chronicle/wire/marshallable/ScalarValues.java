/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

import java.io.File;
import java.lang.annotation.RetentionPolicy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.UUID;

/**
 * A simple dynamic enumeration used in ScalarValues.
 */
enum TestEnum {
    ONE,   // Represents the first value
    TWO,   // Represents the second value
    THREE  // Represents the third value
}

@SuppressWarnings("rawtypes")
public class ScalarValues extends SelfDescribingMarshallable {

    public ScalarValues() {
    }

    public ScalarValues(int i) {
        boolean flag = i == 0;
        byte b = (byte) i;
        short s = (short) i;
        char ch = (char) i;
        long l = (long) i * i;
        double d = i * i;

        Boolean flag2 = i != 0;
        Byte b2 = b;
        Short s2 = s;
        Character ch2 = ch;
        Integer i2 = -i;
        Float f2 = (float) i;
        Double d2 = d;
        Long l2 = l;

        Class<?> aClass = ScalarValues.class;
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
        TestEnum dynamicEnum = TestEnum.THREE;
    }
}
