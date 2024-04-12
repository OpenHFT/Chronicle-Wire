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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.nio.ByteBuffer;

/**
 * The InputStreamToWire class provides functionality to read data from an InputStream and convert it to a Wire representation.
 * This class encapsulates Bytes, Wire, and DataInputStream instances to achieve this conversion, ensuring the data
 * integrity and readability in the wire format.
 */
public class InputStreamToWire {

    // Bytes object used to hold data with elastic memory management
    private final Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(128);

    // Wire object responsible for representing the data in wire format
    private final Wire wire;

    // DataInputStream object to read binary data from an input stream
    private final DataInputStream dis;

    /**
     * Constructor for the InputStreamToWire class, initializing it with the specified WireType and InputStream.
     *
     * @param wireType The type of wire to use for data representation.
     * @param is The input stream from which data will be read.
     */
    public InputStreamToWire(WireType wireType, InputStream is) {
        wire = wireType.apply(bytes);
        dis = new DataInputStream(is);
    }

    /**
     * Reads data from the encapsulated DataInputStream and populates the Wire object with it.
     * The method first clears any existing data in the wire, then reads the data's length from the stream.
     * It then ensures sufficient capacity in the bytes object, reads the binary data into the bytes, and sets the
     * appropriate read position. Finally, it returns the populated wire.
     *
     * @return The populated Wire object.
     * @throws IOException If any I/O errors occur.
     * @throws StreamCorruptedException If the read data length is negative, indicating a corrupted stream.
     */
    public Wire readOne() throws IOException {
        wire.clear();
        int length = dis.readInt();
        if (length < 0) throw new StreamCorruptedException();
        bytes.ensureCapacity(length);
        byte[] array = bytes.underlyingObject().array();
        dis.readFully(array, 0, length);
        bytes.readPositionRemaining(0, length);
        return wire;
    }
}
