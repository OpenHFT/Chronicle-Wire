/*
 * Copyright 2016-2022 chronicle.software
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

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

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
