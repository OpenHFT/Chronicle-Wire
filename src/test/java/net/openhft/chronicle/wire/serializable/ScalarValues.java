package net.openhft.chronicle.wire.serializable;

import net.openhft.chronicle.wire.Wires;

import java.io.File;
import java.io.Serializable;
import java.lang.annotation.RetentionPolicy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.UUID;

import static net.openhft.chronicle.wire.WireType.TEXT;

/**
 * Created by peter on 09/05/16.
 */
public class ScalarValues implements Serializable {
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
//    Path path;

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

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ScalarValues && Wires.isEquals(this, obj);
    }

    @Override
    public String toString() {
        return TEXT.asString(this);
    }
}
