/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

// Test class extending WireTestCommon to verify behavior of method writer with invalid interface
public class MethodWriterMarshallableTest extends WireTestCommon {

    // Test method expecting an IllegalArgumentException when using an invalid interface with method writer
    @Test
    @DisplayName("Invalid interface is rejected by method writer")
    public void invalidInterface() {
        assertThrows(IllegalArgumentException.class, () -> {
            // Create a new wire instance with the TEXT wire type
            Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

            // Attempt to create a method writer for a bad interface, expecting an exception
            wire.methodWriter(MyBadInterface.class);
        }, "Method writer should reject interfaces extending Marshallable");
    }

    // Interface declared invalid for method writer usage due to it extending Marshallable
    private interface MyBadInterface extends Marshallable {
    }
}
