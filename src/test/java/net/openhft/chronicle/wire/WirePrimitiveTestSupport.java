/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
final class WirePrimitiveTestSupport {
    private WirePrimitiveTestSupport() {
    }

    static void assertBooleanRoundTrip(Wire wire) {
        wire.write().bool(false)
                .write().bool(true)
                .write().bool(null);

        wire.read().bool(false, (expected, actual) -> assertEquals(expected, actual))
                .read().bool(true, (expected, actual) -> assertEquals(expected, actual))
                .read().bool(null, (expected, actual) -> assertEquals(expected, actual));
    }

    static void assertFloat32RoundTrip(Wire wire, Object testInstance) {
        wire.write().float32(0.0F)
                .write().float32(Float.NaN)
                .write().float32(Float.POSITIVE_INFINITY)
                .write().float32(Float.NEGATIVE_INFINITY)
                .write().float32(123456.0f);

        wire.read().float32(testInstance, (o, t) -> assertEquals(0.0F, t, 0.0F))
                .read().float32(testInstance, (o, t) -> assertTrue(Float.isNaN(t)))
                .read().float32(testInstance, (o, t) -> assertEquals(Float.POSITIVE_INFINITY, t, 0.0F))
                .read().float32(testInstance, (o, t) -> assertEquals(Float.NEGATIVE_INFINITY, t, 0.0F))
                .read().float32(testInstance, (o, t) -> assertEquals(123456.0f, t, 0.0F));
    }

    static void writeTimes(Wire wire, LocalTime now) {
        wire.write().time(now)
                .write().time(LocalTime.MAX)
                .write().time(LocalTime.MIN);
    }

    static void assertTimes(Wire wire, LocalTime now) {
        wire.read().time(now, (expected, actual) -> assertEquals(expected, actual))
                .read().time(LocalTime.MAX, (expected, actual) -> assertEquals(expected, actual))
                .read().time(LocalTime.MIN, (expected, actual) -> assertEquals(expected, actual));
    }

    static String expectedTimeString(LocalTime now) {
        return "\"\": " + now + "\n" +
                "\"\": 23:59:59.999999999\n" +
                "\"\": 00:00\n";
    }
}
