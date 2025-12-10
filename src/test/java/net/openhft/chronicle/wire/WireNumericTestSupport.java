/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;

final class WireNumericTestSupport {
    private WireNumericTestSupport() {
    }

    static void writeInt64s(Wire wire) {
        wire.write().int64(1);
        wire.write(BWKey.field1).int64(2);
        wire.write(() -> "Test").int64(3);
    }

    static void assertInt64sRead(Wire wire, boolean expectDebug) {
        @NotNull AtomicLong i = new AtomicLong();
        LongStream.rangeClosed(1, 3).forEach(e -> {
            wire.read().int64(i, AtomicLong::set);
            assertEquals(e, i.get());
        });
        assertEquals(0, wire.bytes().readRemaining());
        wire.read();
    }

    static void writeFloat64s(Wire wire) {
        wire.write().float64(1);
        wire.write(BWKey.field1).float64(2);
        wire.write(() -> "Test").float64(3);
    }

    static void assertFloat64sRead(Wire wire) {
        assertEquals(1, wire.read().float64(), 0.0);
        assertEquals(2, wire.read(BWKey.field1).float64(), 0.0);
        assertEquals(3, wire.read().float64(), 0.0);
        assertEquals(0, wire.bytes().readRemaining());
        wire.read();
    }
}
