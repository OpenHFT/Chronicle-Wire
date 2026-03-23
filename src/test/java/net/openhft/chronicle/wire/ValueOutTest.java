/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.*;

public class ValueOutTest extends WireTestCommon {

    private WireType wireType;

    // Provide parameters to be injected into the test class constructor
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {WireType.TEXT},
                {WireType.BINARY}
        });
    }

    // Test the writing and reading of a byte array using the specified WireType
    @ParameterizedTest
    @MethodSource("data")
    public void test(WireType wireType) {
        this.wireType = wireType;
        // Apply the wire type and ensure padding is used if binary
        Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        wire.usePadding(wire.isBinary());

        // Define a byte array to be written and read during the test
        @NotNull final byte[] expected = "this is my byte array".getBytes(ISO_8859_1);
        wire.writeDocument(false, w ->
                w.write().object(expected)

        );

       // System.out.println(Wires.fromSizePrefixedBlobs(wire.bytes()));
       // Verify that the read byte array matches the written byte array
        wire.readDocument(null, w -> {
            @NotNull final byte[] actual = (byte[]) w.read().object();
            assertArrayEquals(expected, actual);

        });

        // Release resources allocated for the byte buffer
        wire.bytes().releaseLast();
    }

    // Test that object serialization and deserialization work as expected
    // when specifying the desired type explicitly
    @ParameterizedTest
    @MethodSource("data")
    public void testRequestedType(WireType wireType) {
        this.wireType = wireType;
        // Initialize the Wire object and enable padding for binary format
        Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        wire.usePadding(wire.isBinary());

        // Define and write a byte array to the Wire object
        @NotNull final byte[] expected = "this is my byte array".getBytes(ISO_8859_1);
        wire.writeDocument(false, w -> w.write().object(expected));

       // System.out.println(Wires.fromSizePrefixedBlobs(wire.bytes()));
       // Read the byte array back and ensure it matches the original
        wire.readDocument(null, w -> {
            @Nullable final byte[] actual = w.read().object(byte[].class);
            assertArrayEquals(expected, actual);
        });

        // Free up resources related to the byte buffer
        wire.bytes().releaseLast();
    }

    // Test the serialization and deserialization of all possible byte values
    @ParameterizedTest
    @MethodSource("data")
    public void testAllBytes(WireType wireType) {
        this.wireType = wireType;
        // Apply the wire type, ensuring padding is applied if binary
        Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        wire.usePadding(wire.isBinary());

        // Loop through all possible byte values and test each one
        for (int i = -128; i < 127; i++) {
            // Create and write a single-byte array to the Wire object
            @NotNull final byte[] expected = {(byte) i};
            wire.writeDocument(false, w ->
                    w.write().object(expected)
            );

            // Ensure the byte array is written and retrievable
            assertNotNull(
                    Wires.fromSizePrefixedBlobs(wire.bytes()));

            // Read back the byte and validate it against the original
            wire.readDocument(null, w -> {
                @Nullable final byte[] actual = (byte[]) w.read().object();
                assertArrayEquals(expected, actual);
            });

        }

        // Release resources utilized by the byte buffer
        wire.bytes().releaseLast();
    }
}
