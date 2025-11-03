/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
class TwoArrays extends SelfDescribingMarshallable implements Closeable {

    // Represents an array of integer values
    public IntArrayValues ia;

    // Represents an array of long values
    public LongArrayValues la;

    // Transient flag indicating whether the TwoArrays instance is closed or not
    private transient boolean closed;

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
