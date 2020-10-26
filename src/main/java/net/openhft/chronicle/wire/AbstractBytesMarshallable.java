/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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

import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;

/**
 * @deprecated "Use BytesInBinaryMarshallable"
 */
@Deprecated
public class AbstractBytesMarshallable extends AbstractMarshallable {
    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        if (wire instanceof BinaryWire) {
            readMarshallable(((BinaryWire) wire).bytes);
        } else {
            super.readMarshallable(wire);
        }
    }

    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        if (wire instanceof BinaryWire) {
            writeMarshallable(((BinaryWire) wire).bytes);
        } else {
            super.writeMarshallable(wire);
        }
    }
}
