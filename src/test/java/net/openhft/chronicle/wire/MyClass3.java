/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

// Define a class representing a marshallable entity with a single string field 'x'
@SuppressFBWarnings(
        value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
        justification = "Fields are populated via Wire marshalling in tests.")
class MyClass3 extends SelfDescribingMarshallable {
    String x; // Field to store a string value
}
