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

package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.nio.ByteBuffer;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test class that examines the BINARY WireType's ability to serialize
 * and deserialize a string containing a Unicode character (emoji followed by text).
 */
public class WireBug39Test extends WireTestCommon {

    /**
     * Test the serialization and deserialization of a string
     * containing a Unicode character (emoji) using the BINARY WireType.
     * The test checks for consistent serialization and deserialization results.
     */
    @Test
    public void testBinaryEncoding() {
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
        assertEquals("obj1.equals(obj2): ", obj1, obj2);

        // Serialize obj2 into bytes using the BINARY WireType
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        obj2.writeMarshallable(wireType.apply(bytes));

        // Convert the bytes back to string
        final String output = bytes.toString();
       // System.out.println("output: [" + output + "]");

        // Deserialize the string back into obj3 and ensure it matches obj1 and obj2
        obj3.readMarshallable(wireType.apply(Bytes.from(output)));

        assertEquals("obj2.equals(obj3): ", obj1, obj2);

        // Release the resources associated with the byte buffer
        bytes.releaseLast();
    }

    /**
     * Class representing an object that implements Marshallable interface.
     * The object mainly manages a StringBuilder data and provides
     * mechanisms to handle serialization and deserialization using Wire.
     */
    class MarshallableObj implements Marshallable {
        private final StringBuilder builder = new StringBuilder();

        public void clear() {
            builder.setLength(0);
        }

        public void append(CharSequence cs) {
            builder.append(cs);
        }

        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            builder.setLength(0);
            assertNotNull(wire.getValueIn().textTo(builder));
        }

        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            wire.getValueOut().text(builder);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            @NotNull MarshallableObj that = (MarshallableObj) o;

            return builder.toString().equals(that.builder.toString());
        }

        @Override
        public int hashCode() {
            return builder.toString().hashCode();
        }

        @NotNull
        @Override
        public String toString() {
            return builder.toString();
        }
    }
}
