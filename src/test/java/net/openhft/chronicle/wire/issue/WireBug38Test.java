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
import org.junit.Test;

import java.nio.ByteBuffer;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeFalse;

/**
 * Test class to validate the handling of nested objects
 * during serialization and deserialization using the Wire framework.
 */
public class WireBug38Test extends WireTestCommon {

    /**
     * Validates that a nested object can be correctly serialized and deserialized
     * using the TEXT WireType. The test checks for consistent serialization and
     * deserialization results of nested structures.
     */
    @Test
    public void testNestedObj() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        // Define the TEXT WireType and a test string
        @NotNull final WireType wireType = WireType.TEXT;
        @NotNull final String exampleString = "{";

        // Create two instances of our Outer object
        @NotNull final Outer obj1 = new Outer();
        @NotNull final Outer obj2 = new Outer();

        // Append the test string to the inner MarshallableObj of the first Outer object
        obj1.getObj().append(exampleString);

        // Serialize obj1 into bytes using the TEXT WireType
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        obj1.writeMarshallable(wireType.apply(bytes));

        // Convert the bytes back to string
        final String output = bytes.toString();
       // System.out.println("output: [" + output + "]");

        // Deserialize the string back into obj2 and ensure it matches obj1
        obj2.readMarshallable(wireType.apply(Bytes.from(output)));

        assertEquals(obj1, obj2);

        // Release the resources associated with the byte buffer
        bytes.releaseLast();
    }

    /**
     * Class representing an object that implements Marshallable interface.
     * The object primarily manages StringBuilder data and supports read/write operations
     * using Wire.
     */
    static class MarshallableObj implements Marshallable {
        private final StringBuilder builder = new StringBuilder();

        public void clear() {
            builder.setLength(0);
        }

        void append(CharSequence cs) {
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

            @Nullable MarshallableObj that = (MarshallableObj) o;

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

    /**
     * Class representing an outer object that encapsulates an instance
     * of the MarshallableObj. This class provides mechanisms to handle
     * serialization and deserialization of its inner object using Wire.
     */
    static class Outer implements Marshallable {
        private final MarshallableObj obj = new MarshallableObj();

        // Retrieves the internal MarshallableObj
        @NotNull
        MarshallableObj getObj() {
            return obj;
        }

        // Reads the nested MarshallableObj from the wire
        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            wire.read(() -> "obj").marshallable(obj);
        }

        // Writes the nested MarshallableObj to the wire
        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            wire.write(() -> "obj").marshallable(obj);
        }

        // Equality is determined by the equality of the nested MarshallableObj
        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            @Nullable Outer outer = (Outer) o;

            return obj.equals(outer.obj);
        }

        @Override
        public int hashCode() {
            return obj.hashCode();
        }
    }
}
