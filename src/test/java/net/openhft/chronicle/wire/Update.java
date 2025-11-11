/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.util.SerializableUpdater;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;

public enum Update implements SerializableUpdater<AtomicLong> {

    // Enumeration constant that represents an INCREMENT update.
    // It increments the provided AtomicLong by 1.
    INCR {
        @Override
        public void update(@NotNull AtomicLong aLong) {
            aLong.incrementAndGet();
        }
    },

    // Enumeration constant that represents a DECREMENT update.
    // It decrements the provided AtomicLong by 1.
    DECR {
        @Override
        public void update(@NotNull AtomicLong aLong) {
            aLong.decrementAndGet();
        }
    }
}
