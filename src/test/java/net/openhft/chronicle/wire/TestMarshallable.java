/*
 * Copyright 2016-2020 chronicle.software
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

import org.jetbrains.annotations.NotNull;

// Defining a class named TestMarshallable, which extends SelfDescribingMarshallable
// to utilize its self-describing marshallable capabilities
public class TestMarshallable extends SelfDescribingMarshallable {

    // Defining a non-null StringBuilder field named 'name' to store a name value,
    // initializing it to an empty StringBuilder instance
    @NotNull
    private StringBuilder name = new StringBuilder();

    // Defining an integer field named 'count' to store a count value
    private int count;

    // Overriding the readMarshallable method from the parent class to customize
    // how the instance should read its state from a WireIn instance
    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IllegalStateException {
        // Reading a text value from the wire, under the key "name", and appending it to the 'name' field
        wire.read(() -> "name").textTo(name);
        // Reading an int32 value from the wire under the key "count", and storing it in the 'count' field
        count = wire.read(() -> "count").int32();
    }

    // Overriding the writeMarshallable method from the parent class to customize
    // how the instance should write its state to a WireOut instance
    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        // Writing the content of the 'name' field as text to the wire under the key "name"
        wire.write(() -> "name").text(name);
        // Writing the value of the 'count' field as an int32 to the wire under the key "count"
        wire.write(() -> "count").int32(count);

    }

    // Defining a getter method for the 'name' field
    // which returns the current state of the 'name' StringBuilder instance
    @NotNull
    public StringBuilder getName() {
        return name;
    }

    // Defining a setter method for the 'name' field,
    // which clears the current content and appends the given CharSequence
    public void setName(CharSequence name) {
        this.name.setLength(0); // Clearing the current content of 'name'
        this.name.append(name); // Appending the new content to 'name'
    }

    // Defining a getter method for the 'count' field,
    // which returns the current state of the 'count' integer value
    public int getCount() {
        return count;
    }

    // Defining a setter method for the 'count' field,
    // which updates the 'count' field with the provided integer value
    public void setCount(int count) {
        this.count = count;
    }
}
