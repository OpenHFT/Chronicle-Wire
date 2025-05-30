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
 * Base class for configuration beans that are {@link Marshallable}.  When
 * reading, fields not present in the input remain unchanged so a previously
 * applied default configuration can be retained.  When writing, only fields that
 * differ from a supplied default are emitted.
 */
public abstract class AbstractMarshallableCfg extends SelfDescribingMarshallable {

    /**
     * Read configuration values from {@code wire}.  Fields not present in the
     * input are left unchanged so defaults can be merged in.
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
     * Write only those fields that differ from the default configuration to
     * {@code wire}.
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
     * Log and skip any field encountered in the wire that is not defined in this
     * configuration object.
     */
    @Override
    public void unexpectedField(Object event, ValueIn valueIn) throws InvalidMarshallableException {
        // Log a warning about the unexpected field
        Jvm.warn().on(getClass(), "Field " + event + " ignored, was " + valueIn.objectBestEffort());
    }
}
