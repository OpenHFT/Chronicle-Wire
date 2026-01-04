/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class LongConversionExampleB {

    // Initializing static block to add the House class as an alias to CLASS_ALIASES
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(LongConversionExampleB.House.class);
    }

    // Static inner class representing a House with an owner using a specific long conversion
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class House extends SelfDescribingMarshallable {
        @LongConversion(Base64LongConverter.class)
        long owner;

        // Method to set the owner's name which is then converted to its corresponding long value
        void owner(CharSequence owner) {
            this.owner = Base64LongConverter.INSTANCE.parse(owner);
        }
    }

    // Main method to demonstrate the House class functionality
    public static void main(String[] args) {
        House house = new House();
        house.owner("Bill");
        System.out.println(house);
    }
}
