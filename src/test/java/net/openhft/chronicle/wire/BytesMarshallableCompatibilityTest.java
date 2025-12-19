/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public final class BytesMarshallableCompatibilityTest extends WireTestCommon {

    // Test the serialization and deserialization of the Container object using BytesMarshallable
    @Test
    public void shouldSerialiseToBytes() {

        // Instantiate and initialize a Container object
        final Container container = new Container();
        container.number = 17;
        container.label = "non-deterministic";
        container.truth = Boolean.TRUE;

        // Create an elastic heap byte buffer to serialize the Container object into
        final Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(64);

        assumeFalse(Jvm.maxDirectMemory() == 0);
        // Serialize the Container object into the bytes buffer
        container.writeMarshallable(bytes);

        // Create a copy of the Container object and deserialize data from the byte buffer into this copy
        final Container copy = new Container();
        copy.readMarshallable(bytes);

        // Validate that the original and copied containers have identical properties
        assertEquals(container.number, copy.number);
        assertEquals(container.label, copy.label);
        assertEquals(container.truth, copy.truth);
    }

    // Private static class representing a container, extending the capabilities provided by BytesInBinaryMarshallable
    private static final class Container extends BytesInBinaryMarshallable {
        private int number;       // Variable to store a number
        private String label;     // Variable to store a label
        private Boolean truth;    // Variable to store a boolean truth value
    }
}
