/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ValueOutTest {

    private WireType wireType;

    // Constructor to initialize the WireType for testing
    public void initValueOutTest(WireType wireType) {
        this.wireType = wireType;
    }

    // Provide parameters to be injected into the test class constructor
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {WireType.TEXT},
                {WireType.BINARY}
        });
    }

    // Test the writing and reading of a byte array using the specified WireType
    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void test(WireType wireType) {
        initValueOutTest(wireType);
        // Apply the wire type and ensure padding is used if binary
        final Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        wire.usePadding(wire.isBinary());
        try {
            // Define a byte array to be written and read during the test
            @NotNull final byte[] expected = "this is my byte array".getBytes(ISO_8859_1);
            wire.writeDocument(false, w ->
                    w.write().object(expected)

            );

            // Verify that the read byte array matches the written byte array
            final byte[][] actualHolder = {null};
            wire.readDocument(null, w -> actualHolder[0] = (byte[]) w.read().object());
            assertArrayEquals(expected, actualHolder[0], "valueOut: roundtrip wireType=" + wireType);
        } finally {
            // Release resources allocated for the byte buffer
            wire.bytes().releaseLast();
        }
    }

    // Test that object serialization and deserialization work as expected
    // when specifying the desired type explicitly
    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void testRequestedType(WireType wireType) {
        initValueOutTest(wireType);
        // Initialize the Wire object and enable padding for binary format
        final Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        wire.usePadding(wire.isBinary());
        try {
            // Define and write a byte array to the Wire object
            @NotNull final byte[] expected = "this is my byte array".getBytes(ISO_8859_1);
            wire.writeDocument(false, w -> w.write().object(expected));

            // Read the byte array back and ensure it matches the original
            final byte[][] actualHolder = {null};
            wire.readDocument(null, w -> actualHolder[0] = w.read().object(byte[].class));
            assertArrayEquals(expected, actualHolder[0], "valueOut: requestedType wireType=" + wireType);
        } finally {
            // Free up resources related to the byte buffer
            wire.bytes().releaseLast();
        }
    }

    // Test the serialization and deserialization of all possible byte values
    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void testAllBytes(WireType wireType) {
        initValueOutTest(wireType);
        // Apply the wire type, ensuring padding is applied if binary
        final Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
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
