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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireOut;
import net.openhft.chronicle.wire.Wires;
import org.jetbrains.annotations.NotNull;
/**
 * Represents a class with default values, extending the functionality of SelfDescribingMarshallable.
 * This class provides default initialization for its member fields.
 */
@SuppressWarnings("rawtypes")
public class WithDefaults extends SelfDescribingMarshallable {

    // Stores bytes data initialized with the string "Hello"
    Bytes<?> bytes = Bytes.from("Hello");

    // Stores a default text value "Hello"
    String text = "Hello";

    // Default flag set to true
    boolean flag = true;

    // Default numerical value set to the smallest integer value
    int num = Integer.MIN_VALUE;

    // Default numerical value set to the smallest long value
    Long num2 = Long.MIN_VALUE;

    // Default quantity initialized to NaN (Not a Number)
    double qty = Double.NaN;

    /**
     * Writes the marshallable data of this class to the given wire.
     * Utilizes the Wires utility class for the actual write operation.
     *
     * @param wire The wire to write the marshallable data to.
     */
    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        Wires.writeMarshallable(this, wire, false);
    }
}
