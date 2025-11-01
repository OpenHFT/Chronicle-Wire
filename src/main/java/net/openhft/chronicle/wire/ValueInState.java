/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

/**
 * Represents the state associated with a particular input value.
 * This class is primarily designed to manage unexpected inputs and the position of these unexpected values.
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
     * Resets this state, clearing {@link #savedPosition} and removing all
     * recorded unexpected positions.
     */
    public void reset() {
        savedPosition = 0;
        unexpectedSize = 0;
    }

    /**
     * Adds the given {@code wirePosition} to the list of unexpected field starts.
     * Grows the internal array if required.
     *
     * @param wirePosition position of an unexpected field
     */
    public void addUnexpected(long wirePosition) {
        if (unexpectedSize >= unexpected.length) {
            int newSize = unexpected.length * 3 / 2 + 8;
            @NotNull long[] unexpected2 = new long[newSize];
            System.arraycopy(unexpected, 0, unexpected2, 0, unexpected.length);
            unexpected = unexpected2;
        }
        unexpected[unexpectedSize++] = wirePosition;
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
