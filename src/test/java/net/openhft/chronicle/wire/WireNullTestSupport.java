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

        assertNull(wire.read().object(Object.class));
        assertNull(wire.read().object(String.class));
        assertNull(wire.read().object(RetentionPolicy.class));
        assertNull(wire.read().object(circleClass));
        return text;
    }
}
