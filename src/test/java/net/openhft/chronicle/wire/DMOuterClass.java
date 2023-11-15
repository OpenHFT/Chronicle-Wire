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

    // String attribute to store textual data in an instance of DMOuterClass.
    String text;

    // Various primitive data type attributes to demonstrate
    // handling different data types within the class.
    boolean b;
    byte bb;
    short s;
    float f;
    double d;
    long l;
    int i;

    // A List to hold instances of the nested class, DMNestedClass,
    // which demonstrates object aggregation within the outer class.
    @NotNull
    List<DMNestedClass> nested = new ArrayList<>();

    // A Map that associates string keys with instances of DMNestedClass,
    // demonstrating keyed data storage and retrieval within the outer class.
    @NotNull
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
        this.text = text;
        this.b = b;
        this.bb = bb;
        this.d = d;
        this.f = f;
        this.i = i;
        this.l = l;
        this.s = s;
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
