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

package net.openhft.chronicle.wire.utils.dto;

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;

/**
 * Data Transfer Object (DTO) for reading and writing marshallable data.
 * It encapsulates three string properties named 'a', 'b', and 'c'.
 */
public class ReadMarshallableDTO extends SelfDescribingMarshallable {
    String a, b, c; // Three string properties.

    /**
     * Reads the marshallable data from the given WireIn instance.
     * Populates the properties 'a', 'b', and 'c' with the read values.
     *
     * @param wire The WireIn instance from which to read the data.
     * @throws IORuntimeException if an IO error occurs during reading.
     */
    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        a = wire.read("a").text(); // Read and assign the value for 'a'.
        b = wire.read("b").text(); // Read and assign the value for 'b'.
        c = wire.read("c").text(); // Read and assign the value for 'c'.
    }

    /**
     * Writes the properties 'a', 'b', and 'c' to the given WireOut instance.
     *
     * @param wire The WireOut instance to which the data is written.
     */
    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        wire.write("a").text(a); // Write the value of 'a'.
        wire.write("b").text(b); // Write the value of 'b'.
        wire.write("c").text(c); // Write the value of 'c'.
    }
}
