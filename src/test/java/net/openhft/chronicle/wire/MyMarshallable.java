/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An implementation of the SelfDescribingMarshallable to demonstrate
 * custom marshalling and unmarshalling of an object with a single field.
 */
class MyMarshallable extends SelfDescribingMarshallable {

    // The data member to be marshalled and unmarshalled
    @Nullable
    private
    String someData;

    /**
     * Constructor to initialize the data member.
     *
     * @param someData The string data to be set. Can be null.
     */
    MyMarshallable(@Nullable String someData) {
        this.someData = someData;
    }

    /**
     * Custom serialization of the object to the Wire format.
     *
     * @param wire The WireOut instance used for writing the data.
     */
    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        // Write the someData value to the Wire format with the field name "MyField"
        wire.write(() -> "MyField").text(someData);
    }

    /**
     * Custom deserialization of the object from the Wire format.
     *
     * @param wire The WireIn instance used for reading the data.
     * @throws IllegalStateException if any issues occur during reading.
     */
    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IllegalStateException {
        // Read the value of the field named "MyField" from the Wire format
        someData = wire.read(() -> "MyField").text();
    }
}
