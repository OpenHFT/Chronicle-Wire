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
 * Interface for objects that read their state from a {@link Wire} into an
 * existing instance.  Reusing the same object instance can dramatically reduce
 * allocation rates in performance critical code.  For immutable objects or
 * those that require a fresh instance on each load see {@link Demarshallable}.
 */
@FunctionalInterface
@DontChain
public interface ReadMarshallable extends CommonMarshallable {

    /**
     * A no-op instance that simply consumes and discards input.  Useful as a
     * placeholder when a {@code ReadMarshallable} is required but the data is
     * intentionally ignored.
     */
    ReadMarshallable DISCARD = w -> {};

    /**
     * Reads the object's state from the given wire input.
     * Implementations should update the current instance's state based on the content of the wire.
     *
     * @param wire The wire input from which the object's state should be read.
     *
     * @throws IORuntimeException If there's an error reading from the wire.
     * @throws InvalidMarshallableException If the data in the wire is not as expected or invalid.
     */
    void readMarshallable(@NotNull WireIn wire) throws IORuntimeException, InvalidMarshallableException;

    /**
     * Called when a field name is encountered that this object does not expect.
     * The default implementation simply skips the value but subclasses may
     * override to perform validation or error handling.
     *
     * @param event   typically the field name that was not recognised
     * @param valueIn the value to skip or process
     */
    default void unexpectedField(Object event, ValueIn valueIn) throws InvalidMarshallableException {
        valueIn.skipValue();
    }
}
