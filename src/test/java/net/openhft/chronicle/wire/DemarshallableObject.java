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

/**
 * This class represents a demarshallable object with capabilities for both reading from
 * and writing to a wire format, making it suitable for serialization and deserialization tasks.
 */
public class DemarshallableObject implements Demarshallable, WriteMarshallable {
    @NotNull
    final String name;  // Holds the name of the object
    final int value;    // Holds a numeric value associated with the object

    /**
     * Constructor that initializes the object with a given name and value.
     *
     * @param name  The name of the object.
     * @param value The numeric value associated with the object.
     */
    public DemarshallableObject(@NotNull String name, int value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Constructor that reads the object's data from a provided wire format.
     *
     * @param wire The wire input from which to read the object's data.
     */
    public DemarshallableObject(@NotNull WireIn wire) {
        this.name = wire.read(() -> "name").text();
        this.value = wire.read(() -> "value").int32();
    }

    /**
     * Writes the object's data to a provided wire format.
     *
     * @param wire The wire output to which the object's data will be written.
     */
    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        wire.write(() -> "name").text(name)
                .write(() -> "value").int32(value);
    }
}
