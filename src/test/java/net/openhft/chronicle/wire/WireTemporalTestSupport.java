/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.Assertions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@SuppressWarnings("deprecation")
final class WireTemporalTestSupport {
    private WireTemporalTestSupport() {
    }

    static void assertZonedDateTimes(Wire wire) {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime max = ZonedDateTime.of(LocalDateTime.MAX, ZoneId.systemDefault());
        ZonedDateTime min = ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault());
        wire.write().zonedDateTime(now)
                .write().zonedDateTime(max)
                .write().zonedDateTime(min);

        wire.read().zonedDateTime(now, Assertions::assertEquals)
                .read().zonedDateTime(max, Assertions::assertEquals)
                .read().zonedDateTime(min, Assertions::assertEquals);
    }

    static void assertLocalDates(Wire wire) {
        LocalDate now = LocalDate.now();
        wire.write().date(now)
                .write().date(LocalDate.MAX)
                .write().date(LocalDate.MIN);

        wire.read().date(now, Assertions::assertEquals)
                .read().date(LocalDate.MAX, Assertions::assertEquals)
                .read().date(LocalDate.MIN, Assertions::assertEquals);
    }

    static void assertUuids(Wire wire) {
        UUID uuid = UUID.randomUUID();

        wire.write().uuid(uuid)
                .write().uuid(new UUID(0, 0))
                .write().uuid(new UUID(Long.MAX_VALUE, Long.MAX_VALUE));

        wire.read().uuid(uuid, Assertions::assertEquals)
                .read().uuid(new UUID(0, 0), Assertions::assertEquals)
                .read().uuid(new UUID(Long.MAX_VALUE, Long.MAX_VALUE), Assertions::assertEquals);
    }
}
