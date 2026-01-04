/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Test class that examines the BINARY WireType's ability to serialize
 * and deserialize a string containing a Unicode character (emoji followed by text).
 */
class WireBug39Test extends WireTestCommon {

    /**
     * Test the serialization and deserialization of a string
     * containing a Unicode character (emoji) using the BINARY WireType.
     * The test checks for consistent serialization and deserialization results.
     */
    @Test
    @DisplayName("Binary wire should preserve unicode string content")
    void testBinaryEncoding() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for binary wire test");

        // Define the BINARY WireType and a test string (an emoji followed by text)
        @NotNull final WireType wireType = WireType.BINARY;
        @NotNull final String exampleString = "\uD83E\uDDC0 extra";

        // Create three instances of our MarshallableObj
        @NotNull final MarshallableObj obj1 = new MarshallableObj();
        @NotNull final MarshallableObj obj2 = new MarshallableObj();
        @NotNull final MarshallableObj obj3 = new MarshallableObj();

        // Set the test string to two of the objects
        obj1.append(exampleString);
        obj2.append(exampleString);

        // Assert that both objects are the same after the operation
        assertEquals(obj1, obj2, "Binary wire should keep obj1 and obj2 equal after append");

        // Serialize obj2 into bytes using the BINARY WireType
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        obj2.writeMarshallable(wireType.apply(bytes));

        // Convert the bytes back to string
        final String output = bytes.toString();

        // Deserialize the string back into obj3 and ensure it matches obj1 and obj2
        obj3.readMarshallable(wireType.apply(Bytes.from(output)));

        assertEquals(obj1, obj2, "Binary wire should keep obj1 and obj2 equal after round trip");

        // Release the resources associated with the byte buffer
        bytes.releaseLast();
    }

}
