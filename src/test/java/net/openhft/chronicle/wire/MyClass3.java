/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

// Define a class representing a marshallable entity with a single string field 'x'
class MyClass3 extends SelfDescribingMarshallable {
    String x; // Field to store a string value
}
