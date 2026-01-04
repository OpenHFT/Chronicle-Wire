/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.CommonMarshallable;
import net.openhft.chronicle.core.annotation.DontChain;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.jetbrains.annotations.NotNull;

/**
 * This interface represents objects that can be reloaded from a stream by reusing an existing instance.
 * Instead of creating a new object every time the data is read from a stream, instances implementing
 * this interface can update their state based on the stream content, thereby potentially improving performance
 * and reducing garbage.
 * <p>
 * For objects which need to deserialize final fields, consider using the {@link Demarshallable} interface.
 *
 * <p>
 * Example usage might involve reading an object's state from a file or network stream
 * without allocating a new object on each read operation.
 */
@FunctionalInterface
@DontChain
public interface ReadMarshallable extends CommonMarshallable {

    /**
     * An instance that discards wire input when reading values.
     */
    @Deprecated(/* to be removed in 2027 */)
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
     * Handles unexpected fields encountered during the deserialization process.
     * Default behavior is to skip the unexpected value. Override this method if a different behavior is required.
     *
     * @param event   The event or field identifier that was unexpected.
     * @param valueIn The value associated with the unexpected field.
     *
     * @throws InvalidMarshallableException If the unexpected field cannot be processed.
     */
    default void unexpectedField(Object event, ValueIn valueIn) throws InvalidMarshallableException {
        valueIn.skipValue();
    }
}
