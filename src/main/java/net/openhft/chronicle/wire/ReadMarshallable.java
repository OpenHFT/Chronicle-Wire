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

import net.openhft.chronicle.bytes.CommonMarshallable;
import net.openhft.chronicle.core.annotation.DontChain;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.jetbrains.annotations.NotNull;

/**
 * This interface marks an object which can be reloaded from a stream, re-using an
 * existing object. For objects which must deserialize final fields, see {@link Demarshallable}.
 */
@FunctionalInterface
@DontChain
public interface ReadMarshallable extends CommonMarshallable {

    /**
     * An instance of ReadMarshallable that performs no action when reading.
     */
    ReadMarshallable DISCARD = w -> {
    };

    /**
     * Reads the state of this object from the specified wire in a straight line ordered manner.
     *
     * @param wire the wire to read from
     * @throws IORuntimeException        if the stream wasn't ordered or formatted as expected
     * @throws InvalidMarshallableException if an error occurs during demarshalling of this object
     */
    void readMarshallable(@NotNull WireIn wire) throws IORuntimeException, InvalidMarshallableException;

    /**
     * Handles the scenario where an unexpected field is encountered while reading.
     *
     * @param event   the unexpected event encountered
     * @param valueIn the current value being read
     * @throws InvalidMarshallableException if an error occurs while skipping the unexpected value
     */
    default void unexpectedField(Object event, ValueIn valueIn) throws InvalidMarshallableException {
        valueIn.skipValue();
    }
}
