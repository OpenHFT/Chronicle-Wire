//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.converter.Base64;

public class LongConversionExampleA {

    // Initializing static block to add the House class as an alias to CLASS_ALIASES
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(LongConversionExampleA.House.class);
    }

    // Static inner class representing a House with an owner represented in Base64 format
    static class House {
        @Base64
        long owner;

        // Method to set the owner's name which is then converted into its Base64 representation
        void owner(CharSequence owner) {
            this.owner = Base64LongConverter.INSTANCE.parse(owner);
        }

        // Override toString to represent the House object in a readable format
        @Override
        public String toString() {
            return "House{" +
                    "owner=" + owner +
                    '}';
        }
    }

    // Main method to demonstrate the House class functionality
    public static void main(String[] args) {
        House house = new House();
        house.owner("Bill");  // Setting owner's name
        System.out.println(house);  // Printing the house object
    }
}
