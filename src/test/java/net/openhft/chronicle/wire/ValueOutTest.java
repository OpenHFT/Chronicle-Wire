/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import junit.framework.TestCase;
import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

@RunWith(value = Parameterized.class)
public class ValueOutTest extends TestCase {

    private final WireType wireType;

    // Constructor to initialize the WireType for testing
    public ValueOutTest(WireType wireType) {
        this.wireType = wireType;
    }

    // Provide parameters to be injected into the test class constructor
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {WireType.TEXT},
                {WireType.BINARY}
        });
    }

    // Test the writing and reading of a byte array using the specified WireType
    @Test
    public void test() {
        // Apply the wire type and ensure padding is used if binary
        final Wire wire = wireType.apply(Bytes.elasticByteBuffer());
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
            Assert.assertArrayEquals(expected, actual);

        });

        // Release resources allocated for the byte buffer
        wire.bytes().releaseLast();
    }

    // Test that object serialization and deserialization work as expected
    // when specifying the desired type explicitly
    @Test
    public void testRequestedType() {
        // Initialize the Wire object and enable padding for binary format
        final Wire wire = wireType.apply(Bytes.elasticByteBuffer());
        wire.usePadding(wire.isBinary());

        // Define and write a byte array to the Wire object
        @NotNull final byte[] expected = "this is my byte array".getBytes(ISO_8859_1);
        wire.writeDocument(false, w -> w.write().object(expected));

       // System.out.println(Wires.fromSizePrefixedBlobs(wire.bytes()));
       // Read the byte array back and ensure it matches the original
        wire.readDocument(null, w -> {
            @Nullable final byte[] actual = w.read().object(byte[].class);
            Assert.assertArrayEquals(expected, actual);
        });

        // Free up resources related to the byte buffer
        wire.bytes().releaseLast();
    }

    // Test the serialization and deserialization of all possible byte values
    @Test
    public void testAllBytes() {
        // Apply the wire type, ensuring padding is applied if binary
        final Wire wire = wireType.apply(Bytes.elasticByteBuffer());
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
                Assert.assertArrayEquals(expected, actual);
            });

        }

        // Release resources utilized by the byte buffer
        wire.bytes().releaseLast();
    }
}
