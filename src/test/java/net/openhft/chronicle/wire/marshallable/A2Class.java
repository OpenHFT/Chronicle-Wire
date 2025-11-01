/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;

// A2Class extends the functionality of AClass
public class A2Class extends AClass {

    // Constructor that initializes the A2Class by passing arguments to the superclass constructor
    public A2Class(int id, boolean flag, byte b, char ch, short s, int i, long l, float f, double d, String text) {
        super(id, flag, b, ch, s, i, l, f, d, text);
    }

    // Override the writeMarshallable method of the superclass
    @Override
    public void writeMarshallable(@NotNull WireOut out) {
        // Call the superclass implementation for writing marshallable data
        super.writeMarshallable(out);
    }

    // Override the readMarshallable method of the superclass
    @Override
    public void readMarshallable(@NotNull WireIn in) {
        // Call the superclass implementation for reading marshallable data
        super.readMarshallable(in);
    }
}
