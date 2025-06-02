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
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        obj2.writeMarshallable(wireType.apply(bytes));

        // Convert the bytes back to string
        final String output = bytes.toString();
       // System.out.println("output: [" + output + "]");

        // Deserialize the string back into obj3 and ensure it matches obj2
        obj3.readMarshallable(wireType.apply(Bytes.from(output)));

        assertEquals(obj2, obj3);

        // Release the resources associated with the byte buffer
        bytes.releaseLast();
    }

    /**
     * Class representing an object that implements Marshallable interface.
     * The object primarily deals with StringBuilder data and provides mechanisms
     * for reading and writing that data with Wire.
     */
    static class MarshallableObj implements Marshallable {
        private final StringBuilder builder = new StringBuilder();

        // Clears the current content of the builder
        public void clear() {
            builder.setLength(0);
        }

        // Appends a character sequence to the builder
        public void append(CharSequence cs) {
            builder.append(cs);
        }

        // Reads the string value from the wire and sets it to the builder
        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            builder.setLength(0);
            assertNotNull(wire.getValueIn().textTo(builder));
        }

        // Writes the current string value from the builder to the wire
        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            wire.getValueOut().text(builder);
        }

        // Equality is based on the content of the builder
        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            @Nullable MarshallableObj that = (MarshallableObj) o;

            return builder.toString().equals(that.builder.toString());
        }

        // Hashcode derived from the content of the builder
        @Override
        public int hashCode() {
            return builder.toString().hashCode();
        }

        // String representation is the content of the builder
        @NotNull
        @Override
        public String toString() {
            return builder.toString();
        }
    }
}
