package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.BytesMarshaller;

class MyTypes implements Marshallable {
    final StringBuilder text = new StringBuilder();
    boolean b;
    short s;
    double d;
    long l;
    int i;

    void b(boolean b) {
        this.b = b;
    }

    void s(short s) {
        this.s = s;
    }

    void d(double d) {
        this.d = d;
    }

    void l(long l) {
        this.l = l;
    }

    void i(int i) {
        this.i = i;
    }

    @Override
    public void writeMarshallable(WireOut wire) {
        wire.write(Fields.B_FLAG).bool(b)
                .write(Fields.S_NUM).int16(s)
                .write(Fields.D_NUM).float64(d)
                .write(Fields.L_NUM).int64(l)
                .write(Fields.I_NUM).int32(i)
                .write(Fields.TEXT).text(text);
    }

    @Override
    public void readMarshallable(WireIn wire) {
        wire.read(Fields.B_FLAG).bool(x -> b = x)
                .read(Fields.S_NUM).int16(this::s)
                .read(Fields.D_NUM).float64(this::d)
                .read(Fields.L_NUM).int64(this::l)
                .read(Fields.I_NUM).int32(this::i)
                .read(Fields.TEXT).text(text)
        ;
    }

    enum Fields implements WireKey {
        B_FLAG, S_NUM, D_NUM, L_NUM, I_NUM, TEXT;

        @Override
        public int code() {
            return ordinal();
        }
    }


    public static void main(String... ignored) {
        BytesMarshaller<MyTypes> bytesMarshaller = MarshallableBytesMarshaller.of(TextWire::new, MyTypes::new);
    }
}
