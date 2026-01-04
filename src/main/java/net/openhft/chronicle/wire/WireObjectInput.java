/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInput;

/**
 * Adapts a {@link WireIn} source to the {@link ObjectInput} API.
 * Useful when Chronicle Wire data must be consumed via {@code ObjectInput}.
 * Methods delegate to the underlying {@code WireIn} and its {@link ValueIn}.
 * Behaviour may differ from standard Java serialisation depending on the wire format.
 * <p>
 * This class is package-private and used internally.
 */
class WireObjectInput implements ObjectInput {
    private final WireIn wire;
    WireObjectInput(WireIn wire) {
        this.wire = wire;
    }

    /**
     * Reads the next value using {@link ValueIn#object()} from the underlying wire.
     */
    @Nullable
    @Override
    public Object readObject() {
        return wire.getValueIn().object();
    }

    /**
     * Reads the next byte as an unsigned int (0-255) or returns {@code -1} on end of stream.
     */
    @Override
    public int read() {
        if (wire.bytes().readRemaining() <= 0)
            return -1;
        return wire.getValueIn().int8() & 0xFF;
    }

    @Override
    public int read(@NotNull byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Reads up to {@code len} bytes starting at {@code off};
     * throws {@link EOFException} if no data remain.
     */
    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        final long remaining = wire.bytes().readRemaining();
        if (remaining <= 0)
            throw new EOFException("No data remaining to read requested byte range");
        if (len > remaining)
            len = (int) remaining;
        Bytes<?> bytes = Bytes.wrapForWrite(b);
        bytes.writePosition(off);
        bytes.writeLimit((long) off + len);
        wire.getValueIn().bytes(bytes);
        return (int) (bytes.lengthWritten(off));
    }

    /**
     * Skips up to {@code n} bytes and may rewind if negative.
     */
    @Override
    public long skip(long n) {
        @NotNull final Bytes<?> bytes = wire.bytes();
        final long maxRewind = bytes.start() - bytes.readPosition();
        long len = Math.max(maxRewind, Math.min(bytes.readRemaining(), n));
        bytes.readSkip(len);
        return len;
    }

    /**
     * Estimates the number of readable bytes without blocking.
     */
    @Override
    public int available() {
        return (int) Math.min(Integer.MAX_VALUE, wire.bytes().readRemaining());
    }

    /**
     * Close is a no-op because the wire lifecycle is managed externally.
     */
    @Override
    public void close() {
    }

    /** Fills the array or throws {@link EOFException}. */
    @Override
    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    /**
     * Not supported because this adapter uses {@link #read(byte[], int, int)} for bulk reads.
     */
    @Override
    public void readFully(byte[] b, int off, int len) {
        throw new UnsupportedOperationException("readFully is not supported; use read(byte[], int, int) instead");
    }

    /**
     * Not supported: use {@link #skip(long)} to move the read position.
     */
    @Override
    public int skipBytes(int n) {
        throw new UnsupportedOperationException("skipBytes is not supported; use skip(long) instead");
    }

    @Override
    public boolean readBoolean() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            throw new EOFException("No data remaining for boolean value");
        return wire.getValueIn().bool();
    }

    @Override
    public byte readByte() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            throw new EOFException("No data remaining for signed byte value");
        return wire.getValueIn().int8();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            throw new EOFException("No data remaining for unsigned byte value");
        return wire.getValueIn().int8() & 0xFF;
    }

    @Override
    public short readShort() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            throw new EOFException("No data remaining for short value");
        return wire.getValueIn().int16();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            throw new EOFException("No data remaining for unsigned short value");
        return wire.getValueIn().uint16();
    }

    @Override
    public char readChar() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            throw new EOFException("No data remaining for character value");
        return (char) wire.getValueIn().int16();
    }

    @Override
    public int readInt() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            throw new EOFException("No data remaining for int value");
        return wire.getValueIn().int32();
    }

    @Override
    public long readLong() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            throw new EOFException("No data remaining for long value");
        return wire.getValueIn().int64();
    }

    @Override
    public float readFloat() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            throw new EOFException("No data remaining for float value");
        return wire.getValueIn().float32();
    }

    @Override
    public double readDouble() throws IOException {
        if (wire.bytes().readRemaining() <= 0)
            throw new EOFException("No data remaining for double value");
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
            throw new EOFException("No data remaining for UTF text value");
        return wire.getValueIn().text();
    }
}
