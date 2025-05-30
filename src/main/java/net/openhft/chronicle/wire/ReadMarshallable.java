/*
 * Copyright 2016-2025 chronicle.software
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
 * Represents objects that can reload their state from a wire by reusing the
 * current instance.  This avoids allocating a new object each time data is
 * read and can reduce garbage collection in performance-critical code.
 * <p>
 * For objects which need to deserialize final fields, consider using the
 * {@link Demarshallable} interface instead.
 */
@FunctionalInterface
@DontChain
public interface ReadMarshallable extends CommonMarshallable {

    /**
     * A no-operation {@code ReadMarshallable} that consumes and discards
     * the input from the wire.  Useful as a placeholder or when unwanted
     * data should be skipped.
     */
    ReadMarshallable DISCARD = w -> {};

    /**
     * Read data from the wire and apply it to this instance.
     * Implementations must parse the input and update their fields
     * accordingly.
     *
     * @param wire the wire to read from
     * @throws IORuntimeException         if an I/O error occurs
     * @throws InvalidMarshallableException if the data is invalid for this type
     */
    void readMarshallable(@NotNull WireIn wire)
            throws IORuntimeException, InvalidMarshallableException;

    /**
     * Handles an unexpected field during deserialization.  The default
     * implementation skips the value.
     *
     * @param fieldName the identifier of the field, typically a String or
     *                  {@code WireKey}
     * @param valueIn   the unexpected value
     * @throws InvalidMarshallableException if the field cannot be processed
     */
    default void unexpectedField(Object fieldName, ValueIn valueIn)
            throws InvalidMarshallableException {
        valueIn.skipValue();
    }
}
