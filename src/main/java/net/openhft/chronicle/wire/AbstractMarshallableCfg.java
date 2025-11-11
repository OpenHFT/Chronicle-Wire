/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.jetbrains.annotations.NotNull;

/**
 * An abstract class that represents a configuration Data Transfer Object (DTO) capable of marshalling.
 * This base class offers default implementations for reading and writing configurations through {@code WireMarshaller}.
 * Derived configuration DTOs can benefit from merging default configurations with specific configurations to enhance flexibility.
 */
public abstract class AbstractMarshallableCfg extends SelfDescribingMarshallable {

    /**
     * Reads the state of this configuration object from the given wire input.
     * It uses the {@link WireMarshaller} corresponding to the class of the current object
     * to perform the reading.
     * <p>
     * Configuration merging is supported; absent values in the wire input retain their
     * existing state. To ensure the use of default values for any unset attributes,
     * consider invoking {@code reset()} before this method.
     *
     * @param wire Wire input source for reading the configuration.
     * @throws IORuntimeException             If there's an IO-related exception during reading.
     * @throws InvalidMarshallableException   If a marshalling error occurs.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException, InvalidMarshallableException {
        // Obtain the WireMarshaller for the current class
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(this.getClass());

        // Use the WireMarshaller to read the configuration
        // Field that are not present in the input are not touched.
        wm.readMarshallable(this, wire, false);
    }

    /**
     * Writes the state of this configuration object to the given wire output.
     * It uses the {@link WireMarshaller} corresponding to the class of the current object
     * to perform the writing.
     * <p>
     * For brevity, only fields differing from default values are written.
     *
     * @param wire Wire output target for writing the configuration.
     * @throws InvalidMarshallableException If a marshalling error occurs.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void writeMarshallable(@NotNull WireOut wire) throws InvalidMarshallableException {
        // Obtain the WireMarshaller for the current class
        WireMarshaller marshaller = WireMarshaller.WIRE_MARSHALLER_CL.get(this.getClass());

        // Use the WireMarshaller to write the configuration
        // Fields with a default value are not written
        marshaller.writeMarshallable(this, wire, false);
    }

    /**
     * Handles the presence of unexpected fields during the reading process.
     * A warning is logged with details of the unexpected field.
     *
     * @param event   The name or identifier of the unexpected field.
     * @param valueIn The value associated with the unexpected field.
     * @throws InvalidMarshallableException If there's an error during the processing.
     */
    @Override
    public void unexpectedField(Object event, ValueIn valueIn) throws InvalidMarshallableException {
        // Log a warning about the unexpected field
        Jvm.warn().on(getClass(), "Field " + event + " ignored, was " + valueIn.objectBestEffort());
    }
}
