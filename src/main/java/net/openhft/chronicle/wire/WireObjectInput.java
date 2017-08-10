/*
 * Copyright 2016 higherfrequencytrading.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInput;

/*
 * Created by Peter Lawrey on 09/05/16.
 */
class WireObjectInput implements ObjectInput {
    private final WireIn wire;

    WireObjectInput(WireIn wire) {
        this.wire = wire;
    }

    @Nullable
    @Override
    public Object readObject() throws ClassNotFoundException, IOException {
        return wire.getValueIn().object();
    }

    @Override
    public int read() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            return -1;
        return wire.getValueIn().int8() & 0xFF;
    }

    @Override
    public int read(@NotNull byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        final long remaining = wire.bytes().readRemaining();
        if (remaining <= 0)
            throw new EOFException();
        if (len > remaining)
            len = (int) remaining;
        Bytes bytes = Bytes.wrapForWrite(b);
        bytes.writePosition(off);
        bytes.writeLimit(off + len);
        wire.getValueIn().bytes(bytes);
        return (int) (bytes.writePosition() - off);
    }

    @Override
    public long skip(long n) throws IOException {
        @NotNull final Bytes<?> bytes = wire.bytes();
        final long maxRewind = bytes.start() - bytes.readPosition();
        long len = Math.max(maxRewind, Math.min(bytes.readRemaining(), n));
        bytes.readSkip(len);
        return len;
    }

    @Override
    public int available() throws IOException {
        return (int) Math.min(Integer.MAX_VALUE, wire.bytes().readRemaining());
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public int skipBytes(int n) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean readBoolean() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            throw new EOFException();
        return wire.getValueIn().bool();
    }

    @Override
    public byte readByte() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            throw new EOFException();
        return wire.getValueIn().int8();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            throw new EOFException();
        return wire.getValueIn().int8() & 0xFF;
    }

    @Override
    public short readShort() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            throw new EOFException();
        return wire.getValueIn().int16();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            throw new EOFException();
        return wire.getValueIn().uint16();
    }

    @Override
    public char readChar() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            throw new EOFException();
        return (char) wire.getValueIn().int16();
    }

    @Override
    public int readInt() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            throw new EOFException();
        return wire.getValueIn().int32();
    }

    @Override
    public long readLong() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            throw new EOFException();
        return wire.getValueIn().int64();
    }

    @Override
    public float readFloat() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            throw new EOFException();
        return wire.getValueIn().float32();
    }

    @Override
    public double readDouble() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            throw new EOFException();
        return wire.getValueIn().float64();
    }

    @NotNull
    @Override
    public String readLine() throws IOException {
        return readUTF();
    }

    @NotNull
    @Override
    public String readUTF() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            throw new EOFException();
        return wire.getValueIn().text();
    }
}
