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

import java.util.Arrays;

/**
 * Represents a collection of offsets used in a YAML structure.
 * The class provides mechanisms to add, retrieve, and manipulate the offsets,
 * which can be useful for various YAML parsing and generation tasks.
 *
 * <p>Internally, this class employs a dynamic array resizing strategy to accommodate
 * varying numbers of offsets without a significant overhead in space.
 *
 * @since 2023-08-29
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
     * Returns the current number of offsets stored in the collection.
     *
     * @return The count of offsets.
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
     * Removes the offset at the specified index.
     *
     * <p>Subsequent offsets are shifted to the left (their indices decrease by one).
     *
     * @param i The index of the offset to be removed.
     */
    public void removeIndex(int i) {
        count--;
        int length = count - i;
        if (length > 0)
            System.arraycopy(offsets, i + 1, offsets, i, length);
        offsets[count] = 0;
    }
}
