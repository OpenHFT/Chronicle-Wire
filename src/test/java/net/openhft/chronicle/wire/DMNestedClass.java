/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

/**
 * This class acts as a nested data structure
 * which is self-describing, enabling simplified marshalling and unmarshalling
 * while interacting with byte streams or while serialization.
 */
class DMNestedClass extends SelfDescribingMarshallable {

    // A string attribute to hold textual data within the instance of DMNestedClass.
    private String str;

    // An integer attribute to hold numerical data within the instance of DMNestedClass.
    private int num;

    /**
     * Parameterized constructor to initialize an instance of DMNestedClass
     * with specified values.
     *
     * @param str a String, representing the textual data to be held by the instance.
     * @param num an int, representing the numerical data to be held by the instance.
     */
    public DMNestedClass(String str, int num) {
        this.str = str;  // Assign the provided string to the instance variable 'str'.
        this.num = num;  // Assign the provided integer to the instance variable 'num'.
    }
}
