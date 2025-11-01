/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

// Define a class representing a marshallable entity containing another custom marshallable object
public class MyClass2 extends SelfDescribingMarshallable {

    MyClass3 myClass; // Field to hold an instance of MyClass3

}
