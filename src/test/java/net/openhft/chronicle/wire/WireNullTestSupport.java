/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import java.lang.annotation.RetentionPolicy;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNull;

final class WireNullTestSupport {
    private WireNullTestSupport() {
    }

    static String writeNulls(Wire wire, Consumer<Wire> nullWriter, Class<?> circleClass) {
        for (int i = 0; i < 4; i++) {
            nullWriter.accept(wire);
        }

        String text = wire.toString();

        Class<?>[] classes = {Object.class, String.class, RetentionPolicy.class, circleClass};
        for (Class<?> clazz : classes) {
            assertNull(wire.read().object(clazz),
                    "Null value should round trip for " + clazz.getSimpleName());
        }
        return text;
    }
}
