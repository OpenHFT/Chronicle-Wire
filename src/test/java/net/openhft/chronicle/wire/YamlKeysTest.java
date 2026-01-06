/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YamlKeysTest extends WireTestCommon {

    @Test
    @DisplayName("push grows storage and updates count")
    void pushGrowsStorageAndUpdatesCount() {
        YamlKeys keys = new YamlKeys();

        for (int i = 0; i < 8; i++) {
            keys.push(i * 10L);
        }

        assertEquals(8, keys.count(), "YamlKeys should record pushed offsets");
        int capacity = keys.offsets().length;
        assertTrue(capacity >= 8, "YamlKeys offsets length should be >= 8 but was " + capacity);
        assertEquals(70L, keys.offsets()[7], "YamlKeys stores the last pushed offset");
    }

    @Test
    @DisplayName("removeIndex shifts offsets and clears tail")
    void removeIndexShiftsOffsetsAndClearsTail() {
        YamlKeys keys = new YamlKeys();
        keys.push(11L);
        keys.push(22L);
        keys.push(33L);

        keys.removeIndex(1);

        assertEquals(2, keys.count(), "YamlKeys removes the requested index");
        assertEquals(33L, keys.offsets()[1], "YamlKeys shifts entries after removal");
        assertEquals(0L, keys.offsets()[keys.count()], "YamlKeys clears the tail slot");
    }

    @Test
    @DisplayName("reset clears count without reallocating the offsets array")
    void resetClearsCountWithoutReallocating() {
        YamlKeys keys = new YamlKeys();
        keys.push(5L);
        keys.push(10L);

        long[] before = keys.offsets();
        keys.reset();

        assertEquals(0, keys.count(), "YamlKeys.reset should clear the count to zero");
        assertTrue(keys.offsets() == before, "YamlKeys.reset should reuse the existing storage array");
    }
}
