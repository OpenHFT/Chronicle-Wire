/*
 * Copyright 2016-2025 chronicle.software
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
 * Package-private helper used by {@link ValueIn} implementations to keep track of a
 * single value or nested structure as it is read from a wire. Any fields that appear
 * out of order can be stored for later processing.
 */
class ValueInState {

    /** A shared, empty {@code long} array instance used as the initial state for {@link #unexpected}. */
    private static final long[] EMPTY_ARRAY = {};

    /** Stores a position in the wire to return to later. */
    private long savedPosition;

    /** The number of valid entries currently in the {@link #unexpected} array. */
    private int unexpectedSize;

    /**
     * An array storing the starting positions of fields that were encountered in the
     * input wire but not immediately processed.
     */
    @NotNull
    private long[] unexpected = EMPTY_ARRAY;

    /**
     * Resets this state, clearing {@link #savedPosition} and removing all
     * recorded unexpected positions.
     */
    public void reset() {
        savedPosition = 0;
        unexpectedSize = 0;
    }

    /**
     * Adds the given {@code position} to the list of unexpected field starts.
     * Grows the internal array if required.
     *
     * @param position position of an unexpected field
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
     * Stores the given position for later retrieval.
     *
     * @param savedPosition position to save
     */
    public void savedPosition(long savedPosition) {
        this.savedPosition = savedPosition;
    }

    /**
     * Returns the last saved position.
     */
    public long savedPosition() {
        return savedPosition;
    }

    /**
     * Returns the current count of unexpected field positions recorded.
     */
    public int unexpectedSize() {
        return unexpectedSize;
    }

    /**
     * Returns the wire position of the unexpected field at {@code index}.
     */
    public long unexpected(int index) {
        return unexpected[index];
    }

    /**
     * Removes the unexpected field position at index {@code i}, shifting the
     * remaining elements down.
     */
    public void removeUnexpected(int i) {
        int length = unexpectedSize - i - 1;
        if (length > 0)
            System.arraycopy(unexpected, i + 1, unexpected, i, length);
        unexpectedSize--;
    }
}
