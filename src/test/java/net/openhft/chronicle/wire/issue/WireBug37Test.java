/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test class to validate handling of newline characters in string values
 * when working with wires, extending common wire tests.
 */
public class WireBug37Test extends WireTestCommon {

    /**
     * Validates that newline characters within a string value are correctly serialized
     * and deserialized using the TEXT WireType. This test ensures that string data
     * with special characters (like newline) remains consistent through serialization
     * and deserialization.
     */
    @Test
    public void testNewlineInString() {
        // Define the TEXT WireType and a test string containing a newline
        @NotNull final WireType wireType = WireType.TEXT;
        @NotNull final String exampleString = "hello\nworld";

        // Create three instances of our Marshallable object
        @NotNull final MarshallableObj obj1 = new MarshallableObj();
        @NotNull final MarshallableObj obj2 = new MarshallableObj();
        @NotNull final MarshallableObj obj3 = new MarshallableObj();

        // Append the test string to the first two objects
        obj1.append(exampleString);
        obj2.append(exampleString);

        // Ensure that the two objects are equal after the append
        assertEquals(obj1, obj2);

        // Serialize obj2 into bytes using the TEXT WireType
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        obj2.writeMarshallable(wireType.apply(bytes));

        // Convert the bytes back to string
        final String output = bytes.toString();

        // Deserialize the string back into obj3 and ensure it matches obj2
        obj3.readMarshallable(wireType.apply(Bytes.from(output)));

        assertEquals(obj2, obj3);

        // Release the resources associated with the byte buffer
        bytes.releaseLast();
    }

}
