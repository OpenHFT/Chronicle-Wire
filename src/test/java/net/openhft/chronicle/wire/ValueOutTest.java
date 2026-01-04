/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Arrays;
import java.util.Collection;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressFBWarnings(
        value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
        justification = "Fields are populated via Wire marshalling in tests.")
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
    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Round-trips byte array with generic object read")
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
            assertArrayEquals(expected, actualHolder[0],
                    "Byte array should round-trip via generic object read for wireType=" + wireType);
        } finally {
            // Release resources allocated for the byte buffer
            wire.bytes().releaseLast();
        }
    }

    // Test that object serialization and deserialization work as expected
    // when specifying the desired type explicitly
    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Round-trips byte array with requested type")
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
            assertArrayEquals(expected, actualHolder[0],
                    "Byte array should round-trip via requested type for wireType=" + wireType);
        } finally {
            // Free up resources related to the byte buffer
            wire.bytes().releaseLast();
        }
    }

    // Test the serialization and deserialization of all possible byte values
    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    @DisplayName("Round-trips all byte values for each wire type")
    public void testAllBytes(WireType wireType) {
        initValueOutTest(wireType);
        // Apply the wire type, ensuring padding is applied if binary
        final Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        wire.usePadding(wire.isBinary());

        // Loop through all possible byte values and test each one
        for (int index = -128; index < 127; index++) {
            // Create and write a single-byte array to the Wire object
            @NotNull final byte[] expected = {(byte) index};
            wire.writeDocument(false, w ->
                    w.write().object(expected)
            );

            // Ensure the byte array is written and retrievable
            assertNotNull(
                    Wires.fromSizePrefixedBlobs(wire.bytes()),
                    "Size-prefixed blob should be written for wireType=" + wireType + ", index=" + index);

            // Read back the byte and validate it against the original
            final byte[][] actualHolder = {null};
            wire.readDocument(null, w -> actualHolder[0] = (byte[]) w.read().object());
            assertArrayEquals(expected, actualHolder[0],
                    "Byte should round-trip after read for wireType=" + wireType + ", index=" + index);

        }

        // Release resources utilized by the byte buffer
        wire.bytes().releaseLast();
    }
}
