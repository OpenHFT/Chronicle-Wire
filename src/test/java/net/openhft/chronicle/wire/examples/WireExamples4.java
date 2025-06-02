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

package net.openhft.chronicle.wire.examples;

import net.openhft.chronicle.wire.JSONWire;
import net.openhft.chronicle.wire.Wire;

/**
 * Demonstrates the use of the Wire library's method writer and reader functionality,
 * specifically with the `JSONWire` implementation. The example uses an interface `Printer`
 * to serialize and then deserialize a method call, allowing for efficient and readable
 * serialization of method invocations.
 */
public class WireExamples4 {

    /**
     * Functional interface representing a message printer.
     */
    interface Printer {
        /**
         * Print the provided message.
         *
         * @param message Message to be printed.
         */
        void print(String message);
    }

    /**
     * Entry point for the demonstration.
     *
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        // Initialize a new JSON Wire instance
        Wire wire = new JSONWire();

        // Obtain a method writer for the `Printer` interface using the wire instance
        final Printer printer = wire.methodWriter(Printer.class);

        // Invoke the `print` method, which will be serialized to the wire
        printer.print("hello world");

        // Print the content of the wire to see the serialized method call
        System.out.println(wire.bytes());

        // Create a method reader from the wire and bind it to the system print method,
        // then read and execute one method from the wire
        wire.methodReader((Printer) System.out::println).readOne();
    }
}
