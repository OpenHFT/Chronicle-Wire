/*
 * Copyright 2016-2020 chronicle.software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.DynamicEnum;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;

import java.io.File;
import java.lang.annotation.RetentionPolicy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.UUID;

@SuppressWarnings("rawtypes")
public class ScalarValues extends SelfDescribingMarshallable {
    boolean flag;
    byte b;
    short s;
    char ch;
    int i;
    float f;
    long l;
    double d;

    Boolean flag2;
    Byte b2;
    Short s2;
    Character ch2;
    Integer i2;
    Float f2;
    Long l2;
    Double d2;

    Class aClass;
    RetentionPolicy policy;
    String text;
    LocalDate date;
    LocalTime time;
    LocalDateTime dateTime;
    ZonedDateTime zonedDateTime;
    UUID uuid;
    BigInteger bi;
    BigDecimal bd;
    File file;
    TestDynamicEnum dynamicEnum;

   // Path path;

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
        dynamicEnum = TestDynamicEnum.THREE;
    }
}

enum TestDynamicEnum implements DynamicEnum {
    ONE,
    TWO,
    THREE
}
