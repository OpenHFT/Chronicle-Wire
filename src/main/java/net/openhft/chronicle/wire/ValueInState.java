/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

/**
 * Package-private helper used by {@link ValueIn} implementations to track parsing state.
 * It records positions of any fields encountered out of order so they can be revisited later.
 */
class ValueInState {

    /** Shared empty array used to initialise {@link #unexpected}. */
    private static final long[] EMPTY_ARRAY = {};

    /** Position in the wire to return to. */
    private long savedPosition;

    /** Number of unexpected field positions currently stored. */
    private int unexpectedSize;

    /** Wire positions of fields encountered but not yet processed. */
    @NotNull
    private long[] unexpected = EMPTY_ARRAY;

    /**
     * Clears the saved position and any recorded unexpected field positions.
     */
    public void reset() {
        savedPosition = 0;
        unexpectedSize = 0;
    }

    /**
     * Records an unexpected field start position so it can be processed later.
     *
     * @param position wire position of the unexpected field
     */
    public void addUnexpected(long position) {
        if (unexpectedSize >= unexpected.length) {
            int newSize = unexpected.length * 3 / 2 + 8;
            @NotNull long[] unexpected2 = new long[newSize];
            System.arraycopy(unexpected, 0, unexpected2, 0, unexpected.length);
            unexpected = unexpected2;
        }
        unexpected[unexpectedSize++] = position;
    }

    /**
     * Sets the wire position to return to later.
     *
     * @param savedPosition position in the underlying wire
     */
    public void savedPosition(long savedPosition) {
        this.savedPosition = savedPosition;
    }

    /**
     * Returns the currently saved wire position.
     */
    public long savedPosition() {
        return savedPosition;
    }

    /**
     * Number of unexpected positions currently recorded.
     */
    public int unexpectedSize() {
        return unexpectedSize;
    }

    /**
     * Returns the unexpected field position at the given index.
     */
    public long unexpected(int index) {
        return unexpected[index];
    }

    /**
     * Removes the unexpected position at the supplied index.
     *
     * @param i index of the entry to remove
     */
    public void removeUnexpected(int i) {
        int length = unexpectedSize - i - 1;
        if (length > 0)
            System.arraycopy(unexpected, i + 1, unexpected, i, length);
        unexpectedSize--;
    }
}
