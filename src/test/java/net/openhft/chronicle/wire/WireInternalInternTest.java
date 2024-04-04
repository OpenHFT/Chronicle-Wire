/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        Class<?> type = ClassAliasPool.CLASS_ALIASES.forName(
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
