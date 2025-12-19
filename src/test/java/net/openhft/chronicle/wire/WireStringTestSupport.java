/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class WireStringTestSupport {
    private WireStringTestSupport() {
    }

    static void writeStrings(Wire wire, String name) {
        wire.write().text("Hello");
        wire.write(BWKey.field1).text("world");
        wire.write(() -> "Test")
                .text(name);
    }

    static void assertReadStrings(Wire wire, String name) {
        @NotNull StringBuilder sb = new StringBuilder();
        for (String expected : new String[]{"Hello", "world", name}) {
            assertNotNull(wire.read().textTo(sb));
            assertEquals(expected, sb.toString());
        }
    }
}
