//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireOut;
import net.openhft.chronicle.wire.Wires;
import org.jetbrains.annotations.NotNull;
/**
 * Represents a class with default values, extending the functionality of SelfDescribingMarshallable.
 * This class provides default initialization for its member fields.
 */
@SuppressWarnings("rawtypes")
class WithDefaults extends SelfDescribingMarshallable {

    // Stores bytes data initialized with the string "Hello"
    public Bytes<?> bytes = Bytes.from("Hello");

    // Stores a default text value "Hello"
    public String text = "Hello";

    // Default flag set to true
    public boolean flag = true;

    // Default numerical value set to the smallest integer value
    public int num = Integer.MIN_VALUE;

    // Default numerical value set to the smallest long value
    public Long num2 = Long.MIN_VALUE;

    // Default quantity initialized to NaN (Not a Number)
    public double qty = Double.NaN;

    /**
     * Writes the marshallable data of this class to the given wire.
     * Utilizes the Wires utility class for the actual write operation.
     *
     * @param wire The wire to write the marshallable data to.
     */
    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        Wires.writeMarshallable(this, wire, false);
    }
}
