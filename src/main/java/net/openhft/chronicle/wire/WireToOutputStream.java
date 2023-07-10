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

import net.openhft.chronicle.bytes.Bytes;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
/**
 * This class provides utility methods to write Wire objects to an OutputStream.
 */
public class WireToOutputStream {
    private final Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(128);
    private final Wire wire;
    private final DataOutputStream dos;

    /**
     * Constructs a WireToOutputStream object with a given WireType and OutputStream.
     *
     * @param wireType the WireType to use for writing data
     * @param os the OutputStream to write data to
     */
    public WireToOutputStream(WireType wireType, OutputStream os) {
        wire = wireType.apply(bytes);
        dos = new DataOutputStream(os);
    }

    /**
     * Returns the Wire object being used for writing, after clearing any existing data.
     *
     * @return the Wire object
     */
    public Wire getWire() {
        wire.clear();
        return wire;
    }

    /**
     * Writes the content of the Wire object to the OutputStream.
     * First, the length of the data is written as an integer, followed by the data itself.
     *
     * @throws IOException if an I/O error occurs
     */
    public void flush() throws IOException {
        int length = Math.toIntExact(bytes.readRemaining());
        dos.writeInt(length);
        dos.write(bytes.underlyingObject().array(), 0, length);
    }
}
