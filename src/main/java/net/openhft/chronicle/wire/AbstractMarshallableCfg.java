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

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base for configuration DTOs that are {@link Marshallable}.
 * <p>
 * The provided implementations of {@link #readMarshallable(WireIn)} and
 * {@link #writeMarshallable(WireOut)} support merging with defaults. When
 * reading, fields missing from the input leave their current values unchanged
 * (which may have been initialised by {@link #reset()}). When writing, only
 * fields that differ from the defaults (as supplied to the
 * {@link WireMarshaller}) tend to be emitted.
 */
public abstract class AbstractMarshallableCfg extends SelfDescribingMarshallable {

    /**
     * Reads this configuration from the supplied wire without overwriting fields
     * absent from the input.
     *
     * @param wire source of configuration data
     * @throws IORuntimeException           on IO issues
     * @throws InvalidMarshallableException if marshalling fails
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
     * Writes this configuration to the wire, omitting fields that match the
     * default instance known to the {@link WireMarshaller}.
     *
     * @param wire destination for the configuration
     * @throws InvalidMarshallableException if marshalling fails
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
     * Logs and skips fields that are not defined in this configuration.
     *
     * @param event   field name encountered
     * @param valueIn value of that field
     * @throws InvalidMarshallableException if processing fails
     */
    @Override
    public void unexpectedField(Object event, ValueIn valueIn) throws InvalidMarshallableException {
        // Log a warning about the unexpected field
        Jvm.warn().on(getClass(), "Field " + event + " ignored, was " + valueIn.objectBestEffort());
    }
}
