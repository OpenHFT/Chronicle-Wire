package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.*;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

@RunWith(value = Parameterized.class)
public class WireInternalInternTest extends WireTestCommon {
    static {
        WireInternal.addAliases();
    }

    private final String typeValue;

    public WireInternalInternTest(String typeValue) {
        this.typeValue = typeValue;
    }

    @NotNull
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> combinations() {
        return Stream.of(
//                new Date(),
//                TimeZone.getTimeZone("GMT"),
//                UUID.randomUUID(),
                DayOfWeek.of(1),
                LocalDate.now(),
                LocalDateTime.now(),
                LocalTime.now(),
                Month.of(1)
//                MonthDay.of(1, 2),
//                OffsetDateTime.now(),
//                OffsetTime.now(),
//                Period.ofDays(2),
//                Year.now(),
//                YearMonth.now(),
//                ZonedDateTime.now()
//                ZoneId.of("GMT")
//                ZoneOffset.ofHoursMinutes(5, 30)
        ).map(s -> new Object[]{"!" + ClassAliasPool.CLASS_ALIASES.nameFor(s.getClass()) + " " + s + " "})
                .collect(Collectors.toList());
    }

    //    @Test
    public void intern() {
        int sep = typeValue.indexOf(' ');
        Class type = ClassAliasPool.CLASS_ALIASES.forName(
                typeValue.substring(1, sep));
        String value = typeValue.substring(sep + 1);
        Object value2 = WireInternal.intern(type, value);
        assertEquals(value, value2.toString());
        Object value3 = WireInternal.intern(type, value);
        assertSame(value2, value3);
    }

    @Test
    public void marshallable() {
        Object o = Marshallable.fromString(typeValue);
        Object o2 = Marshallable.fromString(typeValue);
        assertSame(o, o2);
        String s = WireType.TEXT.asString(o);
        assertEquals(typeValue.trim(), s.trim().replaceAll("\"", ""));
    }
}