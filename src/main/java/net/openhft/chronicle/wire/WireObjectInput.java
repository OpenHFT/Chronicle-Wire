package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInput;

/**
 * Created by peter on 09/05/16.
 */
class WireObjectInput implements ObjectInput {
    private final WireIn wire;

    WireObjectInput(WireIn wire) {
        this.wire = wire;
    }

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
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
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
        final Bytes<?> bytes = wire.bytes();
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

    @Override
    public String readLine() throws IOException {
        return readUTF();
    }

    @Override
    public String readUTF() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            throw new EOFException();
        return wire.getValueIn().text();
    }
}
