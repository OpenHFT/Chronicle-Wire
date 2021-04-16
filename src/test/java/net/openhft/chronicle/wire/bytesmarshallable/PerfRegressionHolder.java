package net.openhft.chronicle.wire.bytesmarshallable;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.wire.BytesInBinaryMarshallable;
import net.openhft.chronicle.wire.FieldInfo;

import java.io.File;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.Arrays;
import java.util.List;

public class PerfRegressionHolder {
    String[] s = "1,12,12345,123456789,123456789012,12345678901234567890123".split(",");
    BytesFields bf1 = new BytesFields(s);
    BytesFields bf2 = new BytesFields();

    DefaultBytesFields df1 = new DefaultBytesFields(s);
    DefaultBytesFields df2 = new DefaultBytesFields();


    ReferenceBytesFields rf1 = new ReferenceBytesFields(s);
    ReferenceBytesFields rf2 = new ReferenceBytesFields();

    StringFields sf1 = new StringFields(s);
    StringFields sf2 = new StringFields();

    DefaultStringFields dsf1 = new DefaultStringFields(s);
    DefaultStringFields dsf2 = new DefaultStringFields();

    DefaultUtf8StringFields dusf1 = new DefaultUtf8StringFields(s);
    DefaultUtf8StringFields dusf2 = new DefaultUtf8StringFields();

    DefaultStringFields dsf0 = new DefaultStringFields(new String[6]);

    final Bytes direct = Bytes.allocateElasticDirect();
    final Bytes onHeap = Bytes.allocateElasticOnHeap();

    MappedBytes mapped;

    static volatile int barrier;

    public void doTest(Runnable runnable) {
        File tmpFile = IOTools.createTempFile("regressionTest");
        tmpFile.deleteOnExit();
        try {
            try (MappedBytes mapped = MappedBytes.mappedBytes(tmpFile, 64 << 10)) {
                this.mapped = mapped;
                int runs = 20_000;
                int outlier = Jvm.isArm() ? 500_000 : 100_000;
                long[] times = new long[4];
                for (int i = 0; i < times.length; i++) {
                    long time = 0;
                    for (int r = 0; r < runs; r++) {
                        long start = System.nanoTime();
                        runnable.run();
                        barrier++;
                        long end = System.nanoTime();
                        time += Math.min(outlier, end - start);
                    }
                    System.out.println("times " + i + ": " + time / runs);
                    runs = 100_000;
                    times[i] = time/runs;
                    Jvm.pause(100);
                }
                Arrays.sort(times);
                System.out.println("result: " + times[(times.length-1) / 2] + " us");
            }
        } catch (IOException ioe) {
            throw new AssertionError(ioe);
        }
    }

    static class BytesFields extends BytesInBinaryMarshallable {
        Bytes a = Bytes.allocateElasticOnHeap();
        Bytes b = Bytes.allocateElasticOnHeap();
        Bytes c = Bytes.allocateElasticOnHeap();
        Bytes d = Bytes.allocateElasticOnHeap();
        Bytes e = Bytes.allocateElasticOnHeap();
        Bytes f = Bytes.allocateElasticOnHeap();

        public BytesFields() {
        }

        public BytesFields(String... s) {
            this();
            this.a.append(s[0]);
            this.b.append(s[1]);
            this.c.append(s[2]);
            this.d.append(s[3]);
            this.e.append(s[4]);
            this.f.append(s[5]);
        }
    }

    static class DefaultBytesFields extends BytesFields {
        public DefaultBytesFields() {
        }

        public DefaultBytesFields(String... s) {
            super(s);
        }

        @Override
        public void readMarshallable(BytesIn bytes) throws IORuntimeException, BufferUnderflowException, IllegalStateException {
            read8Bit(bytes, a);
            read8Bit(bytes, b);
            read8Bit(bytes, c);
            read8Bit(bytes, d);
            read8Bit(bytes, e);
            read8Bit(bytes, f);
        }

        protected void read8Bit(BytesIn bytes, Bytes a) {
            bytes.read8bit(a);
        }

        @Override
        public void writeMarshallable(BytesOut bytes) throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException {
            write8Bit(bytes, a);
            write8Bit(bytes, b);
            write8Bit(bytes, c);
            write8Bit(bytes, d);
            write8Bit(bytes, e);
            write8Bit(bytes, f);
        }

        protected void write8Bit(BytesOut bytes, BytesStore a) {
            bytes.write8bit(a);
        }
    }

    static class ReferenceBytesFields extends DefaultBytesFields {
        public ReferenceBytesFields() {
        }

        public ReferenceBytesFields(String... s) {
            super(s);
        }

        @Override
        public void readMarshallable(BytesIn bytes) throws IORuntimeException, BufferUnderflowException, IllegalStateException {
            read8Bit(bytes, a);
            read8Bit(bytes, b);
            read8Bit(bytes, c);
            read8Bit(bytes, d);
            read8Bit(bytes, e);
            read8Bit(bytes, f);
        }

        @Override
        public void writeMarshallable(BytesOut bytes) throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException {
            write8Bit(bytes, a);
            write8Bit(bytes, b);
            write8Bit(bytes, c);
            write8Bit(bytes, d);
            write8Bit(bytes, e);
            write8Bit(bytes, f);
        }

        @Override
        protected void read8Bit(BytesIn bytes, Bytes a) {
            int length = (int) bytes.readStopBit();
            a.clear();
            for (int i = 0; i < length; i++)
                a.writeByte(bytes.readByte());
        }

        @Override
        protected void write8Bit(BytesOut bytes, BytesStore a) {
            final int length = a.length();
            bytes.writeStopBit(length);
            for (int i = 0; i < length; i++)
                bytes.writeByte(a.readByte(i));
        }
    }

    static class StringFields extends BytesInBinaryMarshallable {
        String a = "";
        String b = "";
        String c = "";
        String d = "";
        String e = "";
        String f = "";

        public StringFields() {
        }

        public StringFields(String... s) {
            this();
            this.a = s[0];
            this.b = s[1];
            this.c = s[2];
            this.d = s[3];
            this.e = s[4];
            this.f = s[5];
        }
    }

    static class DefaultStringFields extends StringFields {
        public DefaultStringFields() {
        }

        public DefaultStringFields(String... s) {
            super(s);
        }

        @Override
        public void readMarshallable(BytesIn bytes) throws IORuntimeException, BufferUnderflowException, IllegalStateException {
            a = bytes.read8bit();
            b = bytes.read8bit();
            c = bytes.read8bit();
            d = bytes.read8bit();
            e = bytes.read8bit();
            f = bytes.read8bit();
        }

        @Override
        public void writeMarshallable(BytesOut bytes) throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException {
            bytes.write8bit(a);
            bytes.write8bit(b);
            bytes.write8bit(c);
            bytes.write8bit(d);
            bytes.write8bit(e);
            bytes.write8bit(f);
        }
    }

    static class DefaultUtf8StringFields extends StringFields {
        public DefaultUtf8StringFields() {
        }

        public DefaultUtf8StringFields(String... s) {
            super(s);
        }

        @Override
        public void readMarshallable(BytesIn bytes) throws IORuntimeException, BufferUnderflowException, IllegalStateException {
            a = bytes.readUtf8();
            b = bytes.readUtf8();
            c = bytes.readUtf8();
            d = bytes.readUtf8();
            e = bytes.readUtf8();
            f = bytes.readUtf8();
        }

        @Override
        public void writeMarshallable(BytesOut bytes) throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException {
            bytes.writeUtf8(a);
            bytes.writeUtf8(b);
            bytes.writeUtf8(c);
            bytes.writeUtf8(d);
            bytes.writeUtf8(e);
            bytes.writeUtf8(f);
        }
    }

    public void benchNull() {
        final DefaultStringFields from = this.dsf0;
        final DefaultStringFields to = this.dsf2;

        testAll2(from, to);
    }

    public void benchBytes() {
        testAll(this.df1, this.df2);
    }

    public void benchFields() {
        testAll(this.bf1, this.bf2);
    }

    public void benchRefBytes() {
        testAll(this.rf1, this.rf2);
    }

    public void benchString() {
        testAll(this.dsf1, this.dsf2);
    }

    public void benchUtf8String() {
        testAll(this.dusf1, this.dusf1);
    }

    public void benchRefString() {
        testAll(this.sf1, this.sf2);
    }

    private void testAll(BytesMarshallable from, BytesMarshallable to) {
        onHeap.clear();
        from.writeMarshallable(onHeap);
        to.readMarshallable(onHeap);

        direct.clear();
        from.writeMarshallable(direct);
        to.readMarshallable(direct);

        mapped.clear();
        from.writeMarshallable(mapped);
        to.readMarshallable(mapped);
    }

    private void testAll2(BytesMarshallable from, BytesMarshallable to) {
        onHeap.clear();
        from.writeMarshallable(onHeap);
        for (int j = 0; j < 4; j++) {
            onHeap.readPosition(0);
            to.readMarshallable(onHeap);
        }

        direct.clear();
        from.writeMarshallable(direct);
        for (int j = 0; j < 4; j++) {
            direct.readPosition(0);
            to.readMarshallable(direct);
        }

        mapped.clear();
        from.writeMarshallable(mapped);
        for (int j = 0; j < 4; j++) {
            mapped.readPosition(0);
            to.readMarshallable(mapped);
        }
    }
}
