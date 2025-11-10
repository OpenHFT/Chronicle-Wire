//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;
import net.openhft.chronicle.core.pool.ClassAliasPool;

public class LongConversionExampleB {

    // Initializing static block to add the House class as an alias to CLASS_ALIASES
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(LongConversionExampleB.House.class);
    }

    // Static inner class representing a House with an owner using a specific long conversion
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
