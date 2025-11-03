/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.FieldGroup;
import java.nio.ByteBuffer;
import static net.openhft.chronicle.bytes.Bytes.*;
import static net.openhft.chronicle.wire.WireType.*;
import net.openhft.chronicle.core.pool.ClassAliasPool;

public class LongConversionExampleC {

    // Initializing static block to add the House class as an alias to CLASS_ALIASES
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(House.class);
    }

    // Static inner class representing a House with address details stored in a specific format
    @SuppressWarnings("this-escape")
    static class House extends SelfDescribingMarshallable {
        @FieldGroup("address")
        // 5 longs, each at 8 bytes = 40 bytes, so we can store a String with up to 40 ISO-8859 characters
        private long text4a, text4b, text4c, text4d, text4e;

        // Transient Bytes object to hold address data
        private transient Bytes<House> address = Bytes.forFieldGroup(this, "address");

        // Method to append the address details to the Bytes object
        void address(CharSequence owner) {
            address.append(owner);
        }
    }

    // Main method demonstrating the serialization and deserialization of the House class
    public static void main(String[] args) {
        House house = new House();
        house.address("82 St John Street, Clerkenwell, London");

        // Creates a buffer to store bytes
        final Bytes<ByteBuffer> t = elasticHeapByteBuffer();

        // Define encoding format
        final Wire wire = BINARY.apply(t);

        // Writes the house object to the byte buffer
        wire.getValueOut().object(house);

        // Dumping the contents of the byte buffer
        System.out.println(t.toHexString());
        System.out.println(t);

        // Reading the house object from the byte buffer
        final House object = wire.getValueIn().object(House.class);

        // Printing the address data from the deserialized house object
        System.out.println(object.address);
    }
}
