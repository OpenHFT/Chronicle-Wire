/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.*;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

// This class tests the internal interning behavior of the Wire component.
public class WireInternalInternTest extends WireTestCommon {

    // Static initializer block to add aliases to the WireInternal component upon class loading.
    static {
        WireInternal.addAliases();
    }

    // Field to store the test input value for each test iteration.
    private String typeValue;

    // This method provides a collection of test data to be used in the parameterized test.
    @NotNull
    public static Collection<Object[]> combinations() {
        return Stream.of(
                // A list of date/time related objects for testing.
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
        )
        // Mapping each object to a new Object array with a formatted string.
        .map(s -> new Object[]{"!" + ClassAliasPool.CLASS_ALIASES.nameFor(s.getClass()) + " " + s + " "})
                .collect(Collectors.toList());
    }

    // This test ensures that when values are interned, the same instance is returned for the same input.
    // It's currently commented out, so it won't be executed.
//    @Test
    public void intern() {
        int sep = typeValue.indexOf(' ');
        Class<?> type = ClassAliasPool.CLASS_ALIASES.forName(
                typeValue.substring(1, sep));
        String value = typeValue.substring(sep + 1);
        // Interning the value using WireInternal.
        Object value2 = WireInternal.intern(type, value);
        // Checking that the interned value matches the expected output.
        assertEquals(value, value2.toString());
        // Interning again and checking that the same instance is returned.
        Object value3 = WireInternal.intern(type, value);
        assertSame(value2, value3);
    }

    // This test ensures that the marshallable component behaves as expected.
    @ParameterizedTest
    @MethodSource("combinations")
    public void marshallable(String typeValue) {
        this.typeValue = typeValue;
        // Creating a Marshallable object from the test input value.
        Object o = Marshallable.fromString(typeValue);
        // Creating another instance and ensuring that the same instance is returned.
        Object o2 = Marshallable.fromString(typeValue);
        assertSame(o, o2);
        // Serializing the object to a string and verifying it matches the expected output.
        String s = WireType.TEXT.asString(o);
        assertEquals(typeValue.trim(), s.trim().replaceAll("\"", ""));
    }
}
