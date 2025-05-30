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

import java.util.Arrays;

/**
 * Internal helper class for {@link YamlTokeniser} to manage a list of byte offsets.
 * <p>
 * These offsets mark the starting positions of YAML keys skipped during an initial
 * parse pass. They can then be revisited without rescanning the stream.
 */
public class YamlKeys {
    /** A shared, empty array instance. */
    private static final long[] NO_OFFSETS = {};

    /** The number of valid offsets in {@link #offsets}. */
    int count = 0;

    /** Array storing the offset values; resized as required. */
    long[] offsets = NO_OFFSETS;

    /**
     * Adds {@code offset} to the end of the list, resizing if needed.
     */
    public void push(long offset) {
        if (count == offsets.length) {
            int size = Math.max(7, offsets.length * 2);
            offsets = Arrays.copyOf(offsets, size);
        }
        offsets[count++] = offset;
    }

    /**
     * Returns the number of stored offsets.
     */
    public int count() {
        return count;
    }

    /**
     * Returns the internal array of offsets. Only the first {@link #count} values
     * are valid.
     */
    public long[] offsets() {
        return offsets;
    }

    /**
     * Clears the list while retaining the underlying array for reuse.
     */
    public void reset() {
        count = 0;
    }

    /**
     * Removes the offset at {@code i}, shifting remaining values left.
     */
    public void removeIndex(int i) {
        count--;
        int length = count - i;
        if (length > 0)
            System.arraycopy(offsets, i + 1, offsets, i, length);
        offsets[count] = 0;
    }
}
