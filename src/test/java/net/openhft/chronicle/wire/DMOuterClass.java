/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class DMOuterClass extends the functionality of SelfDescribingMarshallable,
 * thereby adhering to a pattern that facilitates simplified serialization
 * and deserialization processes through its self-describing nature.
 * This outer class also holds instances of a nested class, and demonstrates
 * the handling of various data types and structures.
 */
class DMOuterClass extends SelfDescribingMarshallable {

    // A List to hold instances of the nested class, DMNestedClass,
    // which demonstrates object aggregation within the outer class.
    @NotNull
    final
    List<DMNestedClass> nested = new ArrayList<>();

    // A Map that associates string keys with instances of DMNestedClass,
    // demonstrating keyed data storage and retrieval within the outer class.
    @NotNull
    final
    Map<String, DMNestedClass> map = new LinkedHashMap<>();

    /**
     * Default constructor for DMOuterClass, enabling creation of an
     * instance without providing initial values.
     */
    DMOuterClass() {
        // No initialization code here since we're using default values for instance variables.
    }

    /**
     * Parameterized constructor for DMOuterClass, enabling creation and
     * initialization of an instance with specified values for its attributes.
     *
     * @param text a String, representing textual data to be stored.
     * @param b    a boolean, a flag or binary state information.
     * @param bb   a byte, holding small numerical data or raw binary.
     * @param d    a double, for holding floating-point numbers with double precision.
     * @param f    a float, for holding floating-point numbers.
     * @param i    an int, for holding integer values.
     * @param l    a long, for holding larger integer values.
     * @param s    a short, for holding small integer values.
     */
    public DMOuterClass(String text, boolean b, byte bb, double d, float f, int i, long l, short s) {
        // String attribute to store textual data in an instance of DMOuterClass.
        // Various primitive data type attributes to demonstrate
        // handling different data types within the class.
    }

    /**
     * Override of the equals method to ensure correct equality check for instances
     * of DMOuterClass, adhering to the contract set by the base class and
     * providing a foundation for further customization.
     *
     * @param o the Object to compare against the current instance for equality.
     * @return a boolean, true if equal, otherwise false.
     */
    @Override
    public boolean equals(Object o) {
        // Calling the equals method of the superclass to maintain expected behavior.
        return super.equals(o);
    }
}
