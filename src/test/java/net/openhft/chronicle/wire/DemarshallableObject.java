//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

/**
 * This class represents a demarshallable object with capabilities for both reading from
 * and writing to a wire format, making it suitable for serialization and deserialization tasks.
 */
class DemarshallableObject implements Demarshallable, WriteMarshallable {
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
