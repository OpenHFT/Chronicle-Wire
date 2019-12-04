package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;

public class B2Class extends BClass {
    private static final int MASHALLABLE_VERSION = 1;

    public B2Class(int id, boolean flag, byte b, char ch, short s, int i, long l, float f, double d, String text) {
        super(id, flag, b, ch, s, i, l, f, d, text);
    }

    @Override
    public void writeMarshallable(BytesOut out) {
        super.writeMarshallable(out);
        out.writeStopBit(MASHALLABLE_VERSION);
    }

    @Override
    public void readMarshallable(BytesIn in) {
        super.readMarshallable(in);
        int version = (int) in.readStopBit();
        if (version == MASHALLABLE_VERSION) {
        } else {
            throw new IllegalStateException("Unknown version " + version);
        }
    }
}
