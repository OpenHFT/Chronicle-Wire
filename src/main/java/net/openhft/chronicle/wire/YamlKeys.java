//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

import java.util.Arrays;

/**
 * Represents a collection of offsets used in a YAML structure.
 * The class provides mechanisms to add, retrieve, and manipulate the offsets,
 * which can be useful for various YAML parsing and generation tasks.
 *
 * <p>Internally, this class employs a dynamic array resizing strategy to accommodate
 * varying numbers of offsets without a significant overhead in space.
 */
public class YamlKeys {
    private static final long[] NO_OFFSETS = {};

    // The current number of offsets stored
    int count = 0;

    // The dynamic array of offsets
    long[] offsets = NO_OFFSETS;

    /**
     * Adds a new offset to the collection.
     *
     * @param offset The offset value to be added.
     */
    public void push(long offset) {
        if (count == offsets.length) {
            int size = Math.max(7, offsets.length * 2);
            offsets = Arrays.copyOf(offsets, size);
        }
        offsets[count++] = offset;
    }

    /**
     * Returns the number of stored offsets in the collection.
     */
    public int count() {
        return count;
    }

    /**
     * Retrieves all the stored offsets.
     *
     * @return An array of stored offsets.
     */
    public long[] offsets() {
        return offsets;
    }

    /**
     * Resets the count of offsets to zero.
     * This method does not clear the offset data but allows for reuse of the storage.
     */
    public void reset() {
        count = 0;
    }

    /**
     * Removes the offset at {@code i}, shifting remaining values left by one.
     */
    public void removeIndex(int i) {
        count--;
        int length = count - i;
        if (length > 0)
            System.arraycopy(offsets, i + 1, offsets, i, length);
        offsets[count] = 0;
    }
}
