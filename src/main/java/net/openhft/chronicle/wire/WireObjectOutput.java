package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;

import java.io.ObjectOutput;

/**
 * Created by peter on 09/05/16.
 */
class WireObjectOutput implements ObjectOutput {
    private final WireOut wire;

    WireObjectOutput(WireOut wire) {
        this.wire = wire;
    }

    @Override
    public void writeObject(Object obj) {
        wire.getValueOut().object(obj);
    }

    @Override
    public void write(int b) {
        wire.getValueOut().uint8(b);
    }

    @Override
    public void write(byte[] b) {
        wire.getValueOut().bytes(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if (off == 0 && len == b.length)
            write(b);
        else
            wire.getValueOut().bytes(Bytes.wrapForRead(b).readPositionRemaining(off, len));
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() {

    }

    @Override
    public void writeBoolean(boolean v) {
        wire.getValueOut().bool(v);
    }

    @Override
    public void writeByte(int v) {
        wire.getValueOut().int8(v);
    }

    @Override
    public void writeShort(int v) {
        wire.getValueOut().int16(v);
    }

    @Override
    public void writeChar(int v) {
        wire.getValueOut().uint16(v);
    }

    @Override
    public void writeInt(int v) {
        wire.getValueOut().uint16(v);
    }

    @Override
    public void writeLong(long v) {
        wire.getValueOut().int64(v);
    }

    @Override
    public void writeFloat(float v) {
        wire.getValueOut().float32(v);
    }

    @Override
    public void writeDouble(double v) {
        wire.getValueOut().float64(v);
    }

    @Override
    public void writeBytes(String s) {
        wire.getValueOut().text(s);
    }

    @Override
    public void writeChars(String s) {
        wire.getValueOut().text(s);
    }

    @Override
    public void writeUTF(String s) {
        wire.getValueOut().text(s);
    }
}
