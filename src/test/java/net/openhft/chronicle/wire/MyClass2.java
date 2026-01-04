/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;




// Define a class representing a marshallable entity containing another custom marshallable object
@SuppressFBWarnings(
        value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
        justification = "Fields are populated via Wire marshalling in tests.")
class MyClass2 extends SelfDescribingMarshallable {

    MyClass3 myClass; // Field to hold an instance of MyClass3

}
