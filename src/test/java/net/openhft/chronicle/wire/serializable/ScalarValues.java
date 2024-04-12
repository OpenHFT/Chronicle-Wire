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

@SuppressWarnings("rawtypes")
public class ScalarValues implements Serializable, Validatable {
    private static final long serialVersionUID = 0L;
    // Primitive data type fields
    boolean flag;
    byte b;
    short s;
    char ch;
    int i;
    float f;
    long l;
    double d;

    // Wrapper class fields for primitive types
    Boolean flag2;
    Byte b2;
    Short s2;
    Character ch2;
    Integer i2;
    Float f2;
    Long l2;
    Double d2;

    // Fields of various Java standard library classes
    Class<?> aClass;
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
