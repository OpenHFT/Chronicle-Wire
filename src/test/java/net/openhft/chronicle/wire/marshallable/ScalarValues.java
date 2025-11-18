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

@SuppressWarnings("rawtypes")
public class ScalarValues extends SelfDescribingMarshallable {
    private boolean flag;
    private byte b;
    private short s;
    private char ch;
    private int i;
    private float f;
    private long l;
    private double d;

    private Boolean flag2;
    private Byte b2;
    private Short s2;
    private Character ch2;
    private Integer i2;
    private Float f2;
    private Long l2;
    private Double d2;

    private Class<?> aClass;
    private RetentionPolicy policy;
    private String text;
    private LocalDate date;
    private LocalTime time;
    private LocalDateTime dateTime;
    private ZonedDateTime zonedDateTime;
    private UUID uuid;
    private BigInteger bi;
    private BigDecimal bd;
    private File file;
    private TestEnum dynamicEnum;

    public ScalarValues() {
    }

    public ScalarValues(int i) {
        flag = i == 0;
        b = (byte) i;
        s = (short) i;
        ch = (char) i;
        this.i = i;
        f = i;
        l = i * i;
        d = i * i;

        flag2 = i != 0;
        b2 = b;
        s2 = s;
        ch2 = ch;
        i2 = -i;
        f2 = f;
        d2 = d;
        l2 = l;

        aClass = ScalarValues.class;
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
        dynamicEnum = TestEnum.THREE;
    }
}
/**
 * A simple dynamic enumeration used in ScalarValues.
 */
enum TestEnum {
    ONE,   // Represents the first value
    TWO,   // Represents the second value
    THREE  // Represents the third value
}
