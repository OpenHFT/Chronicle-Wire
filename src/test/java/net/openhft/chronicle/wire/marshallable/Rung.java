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

package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireOut;
import net.openhft.chronicle.wire.Wires;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a rung in a ladder with certain properties such as price, quantity, and delta.
 * Extends the SelfDescribingMarshallable to provide self-describing marshalling capabilities.
 */
class Rung extends SelfDescribingMarshallable {

    // Represents the price of the rung
    double price;

    // Represents the quantity associated with this rung
    double qty;

    // Indicates if this rung is a delta (difference) from the previous rung
    boolean delta;

    // A string field to denote a property that hasn't been set
    String notSet;

    /**
     * The main method to demonstrate the marshalling of the Rung class.
     *
     * @param args Command-line arguments (not used in this demonstration).
     */
    public static void main(String[] args) {
        Rung x = new Rung();
        x.price = 1.234;
        x.qty = 1e6;

        // Print the string representation of the rung object
        System.out.println(x);
    }

    /**
     * Overrides the writeMarshallable method from SelfDescribingMarshallable.
     * Used to write this object's fields to the provided wire.
     *
     * @param wire The wire to which this object's data will be written.
     */
    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        // Use Wires utility to write the object's data to the wire
        Wires.writeMarshallable(this, wire, false);
    }
}
