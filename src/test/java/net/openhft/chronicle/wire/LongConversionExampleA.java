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
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.converter.Base64;

public class LongConversionExampleA {

    // Initializing static block to add the House class as an alias to CLASS_ALIASES
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(LongConversionExampleA.House.class);
    }

    // Static inner class representing a House with an owner represented in Base64 format
    public static class House {
        @Base64
        long owner;

        // Method to set the owner's name which is then converted into its Base64 representation
        public void owner(CharSequence owner) {
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
