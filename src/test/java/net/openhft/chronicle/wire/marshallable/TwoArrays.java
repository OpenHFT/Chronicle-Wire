/*
 * Copyright 2016-2022 chronicle.software
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

package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.ref.BinaryIntArrayReference;
import net.openhft.chronicle.bytes.ref.BinaryLongArrayReference;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.values.IntArrayValues;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;

/**
 * Represents a marshallable object containing two arrays: one of integers and one of longs.
 * Implements the Closeable interface to provide resource management capabilities.
 */
public class TwoArrays extends SelfDescribingMarshallable implements Closeable {

    // Represents an array of integer values
    final IntArrayValues ia;

    // Represents an array of long values
    final LongArrayValues la;

    // Transient flag indicating whether the TwoArrays instance is closed or not
    transient boolean closed;

    /**
     * Constructs a new TwoArrays instance with specified sizes for the integer and long arrays.
     *
     * @param isize Size of the integer array.
     * @param lsize Size of the long array.
     */
    public TwoArrays(int isize, long lsize) {
        this.ia = new BinaryIntArrayReference(isize);
        this.la = new BinaryLongArrayReference(lsize);
    }

    /**
     * Closes the TwoArrays instance and releases any resources associated with the arrays.
     * Marks the instance as closed.
     */
    @Override
    public void close() {
        // Close arrays quietly without throwing exceptions
        Closeable.closeQuietly(ia, la);

        // Set the closed flag to true
        closed = true;
    }

    /**
     * Checks if the TwoArrays instance is closed.
     *
     * @return true if closed, otherwise false.
     */
    @Override
    public boolean isClosed() {
        return closed;
    }
}
