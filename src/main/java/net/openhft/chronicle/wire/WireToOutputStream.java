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
 * Provides a bridge to write structured data in Chronicle Wire format to a
 * standard Java {@link OutputStream}.
 * <p>
 * The class facilitates writing data to an OutputStream using the Wire format. It uses
 * an intermediate {@link Bytes} buffer to temporarily hold data in the Wire format
 * before flushing it to the actual OutputStream. The provided {@link OutputStream} is not
 * closed by this class
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
     * @param wireType the {@link WireType} used to serialise data into the
     *                 internal buffer (for example Binary, Text or JSON).
     * @param os       the {@link OutputStream} to which length-prefixed messages
     *                 will be written when {@link #flush()} is called. This
     *                 stream is not closed by this class.
     */
    public WireToOutputStream(WireType wireType, OutputStream os) {
        wire = wireType.apply(bytes);
        dos = new DataOutputStream(os);
    }

    /**
     * Returns the internal {@link Wire} prepared for a new message.
     * <p>
     * The backing buffer is cleared before the wire is returned. Any data not
     * yet flushed will be lost.
     *
     * @return the cleared {@link Wire} ready for serialisation.
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
     * @throws IOException if an I/O error occurs while writing.
     */
    public void flush() throws IOException {
        int length = Math.toIntExact(bytes.readRemaining());
        dos.writeInt(length);
        dos.write(bytes.underlyingObject().array(), 0, length);
    }
}
