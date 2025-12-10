/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

import static org.junit.Assert.assertEquals;

final class WireReadTestSupport {
    private WireReadTestSupport() {
    }

    static void writeStandardFields(Wire wire) {
        wire.write();
        wire.write(BWKey.field1);
        wire.write(() -> "Test");
    }

    static void exerciseRead(Wire wire, long expectedRemaining) {
        wire.read();
        wire.read();
        wire.read();
        assertEquals(expectedRemaining, wire.bytes().readRemaining());
        wire.read();
    }

    static void exerciseReadWithKey(Wire wire, long expectedRemaining) {
        wire.read(BWKey.field1);
        wire.read(BWKey.field1);
        wire.read(BWKey.field1);
        assertEquals(expectedRemaining, wire.bytes().readRemaining());
        wire.read();
    }

    static void exerciseReadWithNames(Wire wire, String name1, String expectedSecond, String expectedThird, long expectedRemaining) {
        @NotNull StringBuilder name = new StringBuilder();
        wire.read(name);
        assertEquals(0, name.length());

        name.setLength(0);
        wire.read(name);
        assertEquals(expectedSecond, name.toString());

        name.setLength(0);
        wire.read(name);
        assertEquals(expectedThird, name.toString());

        assertEquals(expectedRemaining, wire.bytes().readRemaining());
        wire.read();
    }
}
