/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;

import java.io.ObjectOutput;
import java.util.List;
import java.util.Map;

/**
 * Adapter that writes data to a {@link WireOut} using the {@link java.io.ObjectOutput} API.
 * <p>
 * Calls are forwarded to {@link WireOut#getValueOut()} and encoded according to
 * the underlying wire type. This class is package-private and intended for
 * internal use when third‑party code expects an {@code ObjectOutput}.
 */
class WireObjectOutput implements ObjectOutput {
    // The target wire for all writes
    private final WireOut wire;

    /**
     * @param wire the wire instance to adapt
     */
    WireObjectOutput(WireOut wire) {
        this.wire = wire;
    }

    @Override
    public void writeObject(Object obj) {
        @NotNull final ValueOut valueOut = wire.getValueOut();
        if (obj instanceof Map)
            valueOut.typePrefix(Map.class);
        else if (obj instanceof List)
            valueOut.typePrefix(List.class);
        valueOut.object(obj);
    }

    /**
     * Writes one unsigned byte via .
     */
    @Override
    public void write(int b) {
        wire.getValueOut().uint8(b);
    }

    /**
     * Writes the whole array via {@link ValueOut#bytes(byte[])}.
     */
    @Override
    public void write(byte[] b) {
        wire.getValueOut().bytes(b);
    }

    /**
     * Writes a sub‑array of bytes.
     */
    @Override
    public void write(byte[] b, int off, int len) {
        if (off == 0 && len == b.length)
            write(b);
        else
            wire.getValueOut().bytes(Bytes.wrapForRead(b).readPositionRemaining(off, len));
    }

    /** No-op. */
    @Override
    public void flush() {
        // Do nothing
    }

    /** No-op. */
    @Override
    public void close() {
        // Do nothing
    }

    /**
     * Writes a boolean via .
     */
    @Override
    public void writeBoolean(boolean v) {
        wire.getValueOut().bool(v);
    }

    /**
     * Writes a signed byte via {@link ValueOut#int8(long)}.
     */
    @Override
    public void writeByte(int v) {
        wire.getValueOut().int8(v);
    }

    /**
     * Writes a 16‑bit signed value via {@link ValueOut#int16(long)}.
     */
    @Override
    public void writeShort(int v) {
        wire.getValueOut().int16(v);
    }

    /**
     * Writes a 16‑bit Unicode character via {@link ValueOut#uint16(long)}.
     */
    @Override
    public void writeChar(int v) {
        wire.getValueOut().uint16(v);
    }

    /**
     * Writes a 32‑bit integer via {@link ValueOut#uint16(long)}.
     */
    @Override
    public void writeInt(int v) {
        wire.getValueOut().uint16(v);
    }

    /**
     * Writes a 64‑bit integer via {@link ValueOut#int64(long)}.
     */
    @Override
    public void writeLong(long v) {
        wire.getValueOut().int64(v);
    }

    /**
     * Writes a 32‑bit floating‑point value via {@link ValueOut#float32(float)}.
     */
    @Override
    public void writeFloat(float v) {
        wire.getValueOut().float32(v);
    }

    /**
     * Writes a 64‑bit floating‑point value via {@link ValueOut#float64(double)}.
     */
    @Override
    public void writeDouble(double v) {
        wire.getValueOut().float64(v);
    }

    /**
     * Writes a string in ISO‑8859‑1 via {@link ValueOut#text(CharSequence)}.
     */
    @Override
    public void writeBytes(@NotNull String s) {
        wire.getValueOut().text(s);
    }

    /**
     * Writes a string as a sequence of UTF‑1 characters.
     */
    @Override
    public void writeChars(@NotNull String s) {
        wire.getValueOut().text(s);
    }

    /**
     * Writes a UTF‑8 string via {@link ValueOut#text(CharSequence)}.
     */
    @Override
    public void writeUTF(@NotNull String s) {
        wire.getValueOut().text(s);
    }
}
