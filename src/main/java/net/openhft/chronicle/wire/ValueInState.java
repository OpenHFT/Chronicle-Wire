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
 * Represents the state associated with a particular input value.
 * This class is primarily designed to manage unexpected inputs and the position of these unexpected values.
 *
 * @since 2023-09-09
 */
class ValueInState {

    // A constant representing an empty array of long values
    private static final long[] EMPTY_ARRAY = {};

    // The saved position for the current state
    private long savedPosition;

    // Size of the unexpected values
    private int unexpectedSize;

    // An array to hold unexpected values
    @NotNull
    private long[] unexpected = EMPTY_ARRAY;

    /**
     * Resets the saved position and the unexpected values to their initial states.
     */
    public void reset() {
        savedPosition = 0;
        unexpectedSize = 0;
    }

    /**
     * Adds an unexpected position to the list of unexpected values.
     *
     * @param position The unexpected position to be added
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
     * Sets the saved position for the current state.
     *
     * @param savedPosition The position to be saved
     */
    public void savedPosition(long savedPosition) {
        this.savedPosition = savedPosition;
    }

    /**
     * Retrieves the saved position for the current state.
     *
     * @return The saved position
     */
    public long savedPosition() {
        return savedPosition;
    }

    /**
     * Retrieves the number of unexpected positions stored.
     *
     * @return The size of unexpected positions
     */
    public int unexpectedSize() {
        return unexpectedSize;
    }

    /**
     * Retrieves a specific unexpected position based on its index.
     *
     * @param index The index of the unexpected position
     * @return The unexpected position at the given index
     */
    public long unexpected(int index) {
        return unexpected[index];
    }

    /**
     * Removes an unexpected position from the list based on its index.
     *
     * @param i The index of the unexpected position to be removed
     */
    public void removeUnexpected(int i) {
        int length = unexpectedSize - i - 1;
        if (length > 0)
            System.arraycopy(unexpected, i + 1, unexpected, i, length);
        unexpectedSize--;
    }
}
