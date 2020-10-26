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

import net.openhft.chronicle.bytes.Bytes;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.nio.ByteBuffer;

public class InputStreamToWire {
    private final Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(128);
    private final Wire wire;
    private final DataInputStream dis;

    public InputStreamToWire(WireType wireType, InputStream is) {
        wire = wireType.apply(bytes);
        dis = new DataInputStream(is);
    }

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
