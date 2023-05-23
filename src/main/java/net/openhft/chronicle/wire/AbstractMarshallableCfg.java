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

public abstract class AbstractMarshallableCfg extends SelfDescribingMarshallable {

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException, InvalidMarshallableException {
        WireMarshaller wm = WireMarshaller.WIRE_MARSHALLER_CL.get(this.getClass());
        wm.readMarshallable(this, wire, wm.defaultValue(), true);
    }

    @Override
    public void writeMarshallable(@NotNull WireOut wire) throws InvalidMarshallableException {
        WireMarshaller marshaller = WireMarshaller.WIRE_MARSHALLER_CL.get(this.getClass());
        marshaller.writeMarshallable(this, wire, marshaller.defaultValue(), false);
    }

    @Override
    public void unexpectedField(Object event, ValueIn valueIn) throws InvalidMarshallableException {
        Jvm.warn().on(getClass(), "Field " + event + " ignored, was " + valueIn.objectBestEffort());
    }
}
