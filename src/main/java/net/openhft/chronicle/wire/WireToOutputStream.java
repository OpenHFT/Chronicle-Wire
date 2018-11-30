package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class WireToOutputStream {
    private final Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(128);
    private final Wire wire;
    private final DataOutputStream dos;

    public WireToOutputStream(WireType wireType, OutputStream os) {
        wire = wireType.apply(bytes);
        dos = new DataOutputStream(os);
    }

    public Wire getWire() {
        wire.clear();
        return wire;
    }

    public void flush() throws IOException {
        int length = Math.toIntExact(bytes.readRemaining());
        dos.writeInt(length);
        dos.write(bytes.underlyingObject().array(), 0, length);
    }
}
