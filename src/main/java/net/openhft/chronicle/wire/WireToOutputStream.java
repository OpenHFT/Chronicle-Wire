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
 * This class bridges between a Wire object and a traditional OutputStream.
 * <p>
 * The class facilitates writing data to an OutputStream using the Wire format. It uses
 * an intermediate {@link Bytes} buffer to temporarily hold data in the Wire format
 * before flushing it to the actual OutputStream.
 */
public class WireToOutputStream {

    // Internal byte buffer to temporarily hold data in Wire format.
    private final Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(128);

    // The Wire object responsible for handling the data.
    private final Wire wire;

    // The DataOutputStream to which the data in Wire format is written.
    private final DataOutputStream dos;

    /**
     * Constructs a new instance with the specified WireType and OutputStream.
     *
     * @param wireType The type of Wire to be used.
     * @param os The OutputStream to which the data in Wire format will be written.
     */
    public WireToOutputStream(WireType wireType, OutputStream os) {
        wire = wireType.apply(bytes);
        dos = new DataOutputStream(os);
    }

    /**
     * Retrieves the Wire object for writing data.
     * <p>
     * This method also clears any previous data in the Wire.
     *
     * @return The Wire object to be used for writing data.
     */
    public Wire getWire() {
        wire.clear();
        return wire;
    }

    /**
     * Flushes the data in Wire format to the underlying OutputStream.
     * <p>
     * The method writes the length of the data followed by the actual data to
     * the OutputStream. After the flush, the internal buffer is ready to hold new data.
     *
     * @throws IOException If an I/O error occurs.
     */
    public void flush() throws IOException {
        int length = Math.toIntExact(bytes.readRemaining());
        dos.writeInt(length);
        dos.write(bytes.underlyingObject().array(), 0, length);
    }
}
