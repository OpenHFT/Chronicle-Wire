/*
 * Copyright 2016-2020 chronicle.software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.ReadResolvable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

/**
 * Tests for marshalling and unmarshalling of Enum using Wire.
 */
public class EnumTest extends WireTestCommon {

    /**
     * Tests serialization and deserialization of the TestEnum enumeration.
     */
    @Test
    public void testEnum() {
        // Expecting an exception regarding enum handling
        expectException("Treating class net.openhft.chronicle.wire.EnumTest$TestEnum as enum not WriteMarshallable");

        // Create a byte buffer to work with
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        try {
            // Initialize a Text wire using the byte buffer
            @NotNull Wire wire = WireType.TEXT.apply(bytes);

            // Write the TestEnum.INSTANCE to the wire
            wire.write("test")
                .object(TestEnum.INSTANCE);

            // Validate the serialized form of the TestEnum
            assertEquals("test: !net.openhft.chronicle.wire.EnumTest$TestEnum INSTANCE\n", wire.toString());

            // Create another Text wire with serialized TestEnum
            @NotNull TextWire wire2 = TextWire.from(
                    "test: !net.openhft.chronicle.wire.EnumTest$TestEnum {\n" +
                            "}\n");

            // Deserialize the TestEnum back from the wire
            @Nullable Object enumObject = wire2.read(() -> "test")
                .object();

            // Ensure original and read enum are the same
            Assert.assertSame(TestEnum.INSTANCE, enumObject);
        } finally {
            // Release the byte buffer resources
            bytes.releaseLast();
        }
    }

    /**
     * Enumeration used for testing purposes.
     * Implements Marshallable for Wire compatibility.
     */
    public enum TestEnum implements Marshallable {
        INSTANCE;

        // Read data from the wire, currently no implementation
        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        }

        // Write data to the wire, currently no implementation
        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
        }
    }
}
