/*
 * Copyright 2016-2020 chronicle.software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
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
