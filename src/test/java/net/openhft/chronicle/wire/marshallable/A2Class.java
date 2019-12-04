package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;

public class A2Class extends AClass {
    public A2Class(int id, boolean flag, byte b, char ch, short s, int i, long l, float f, double d, String text) {
        super(id, flag, b, ch, s, i, l, f, d, text);
    }

    @Override
    public void writeMarshallable(WireOut out) {
        super.writeMarshallable(out);
    }

    @Override
    public void readMarshallable(WireIn in) {
        super.readMarshallable(in);
    }
}
