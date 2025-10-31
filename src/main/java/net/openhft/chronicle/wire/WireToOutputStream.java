/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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

    // Internal, elastically-sized heap {@link Bytes} buffer that backs the {@link #wire}. Data is serialised into this buffer before being flushed
    private final Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(128);

    // The internal {@link Wire} instance into which data is serialised. Its type is determined by the {@code wireType} passed to the constructor
    private final Wire wire;

    // The {@link DataOutputStream} wrapping the user-provided {@link OutputStream}, used for conveniently writing primitive data such as the message length
    private final DataOutputStream dos;

    /**
     * Creates a new bridge for the given wire type and output stream.
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
     * Writes the data currently in the wire buffer to the underlying output
     * stream.
     * <p>
     * A four byte length is written first, followed by the serialised bytes.
     * The content remains in the buffer until {@link #getWire()} is called.
     *
     * @throws IOException if an I/O error occurs while writing.
     */
    public void flush() throws IOException {
        int length = Math.toIntExact(bytes.readRemaining());
        dos.writeInt(length);
        dos.write(bytes.underlyingObject().array(), 0, length);
    }
}
