/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Exploratory constructor-materialisation contract for Chronicle-Wire#227.
 * This is not an accepted implementation test until the consumer gate in
 * {@code src/main/docs/immutable-dto-record-support-227.adoc} is satisfied.
 */
public class ImmutableDtoRecordSupportTest {

    @Ignore("Chronicle-Wire#227: proposed contract awaits a named consumer and mapping rules")
    @Test
    public void plainImmutableDtoIsCreatedThroughItsConstructor() {
        final ImmutablePoint point = WireType.TEXT.fromString(
                ImmutablePoint.class, "{ x: 3, y: 4 }");

        assertEquals(3, point.x());
        assertEquals(4, point.y());
        assertEquals(97, point.constructorChecksum());
        assertThrows(IllegalArgumentException.class, () -> WireType.TEXT.fromString(
                ImmutablePoint.class, "{ x: -1, y: 4 }"));
    }

    /**
     * Plain external-style carrier: no Chronicle superclass or interface.
     * The transient checksum cannot be populated by field deserialisation.
     */
    static final class ImmutablePoint {
        private final int x;
        private final int y;
        private final transient int constructorChecksum;

        ImmutablePoint(int x, int y) {
            if (x < 0 || y < 0)
                throw new IllegalArgumentException("coordinates must be non-negative");
            this.x = x;
            this.y = y;
            constructorChecksum = x * 31 + y;
        }

        int x() {
            return x;
        }

        int y() {
            return y;
        }

        int constructorChecksum() {
            return constructorChecksum;
        }
    }
}
