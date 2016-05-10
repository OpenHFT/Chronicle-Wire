package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.AbstractMarshallable;

import java.io.File;
import java.lang.annotation.RetentionPolicy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.UUID;

/**
 * Created by peter on 09/05/16.
 */
public class ScalarValues extends AbstractMarshallable {
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
    }
}
