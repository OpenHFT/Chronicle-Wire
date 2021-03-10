package net.openhft.chronicle.wire.bytesmarshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.BytesInBinaryMarshallable;
import org.junit.Test;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static org.junit.Assert.fail;

public class PerfRegressionTest {

    static class BytesFields extends BytesInBinaryMarshallable {
        Bytes a = Bytes.allocateElasticOnHeap();
        Bytes b = Bytes.allocateElasticOnHeap();
        Bytes c = Bytes.allocateElasticOnHeap();
        Bytes d = Bytes.allocateElasticOnHeap();

        public BytesFields() {
        }

        public BytesFields(String a, String b, String c, String d) {
            this();
            this.a.append(a);
            this.b.append(b);
            this.c.append(c);
            this.d.append(d);
        }
    }

    static class DefaultBytesFields extends BytesFields {
        public DefaultBytesFields() {
        }

        public DefaultBytesFields(String a, String b, String c, String d) {
            super(a, b, c, d);
        }

        @Override
        public void readMarshallable(BytesIn bytes) throws IORuntimeException, BufferUnderflowException, IllegalStateException {
            read8Bit(bytes, a);
            read8Bit(bytes, b);
            read8Bit(bytes, c);
            read8Bit(bytes, d);
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
        }

        protected void write8Bit(BytesOut bytes, BytesStore a) {
            bytes.write8bit(a);
        }
    }

    static class ReferenceBytesFields extends DefaultBytesFields {
        public ReferenceBytesFields() {
        }

        public ReferenceBytesFields(String a, String b, String c, String d) {
            super(a, b, c, d);
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

    @Test
    public void bytesPerformance() {
        BytesFields bf1 = new BytesFields("1", "123", "12345", "12345678901");
        BytesFields bf2 = new BytesFields();

        DefaultBytesFields df1 = new DefaultBytesFields("1", "123", "12345", "12345678901");
        DefaultBytesFields df2 = new DefaultBytesFields();

        ReferenceBytesFields rf1 = new ReferenceBytesFields("1", "123", "12345", "12345678901");
        ReferenceBytesFields rf2 = new ReferenceBytesFields();

        final Bytes bytes = Bytes.allocateElasticDirect();
        final int count = 250_000;
        for (int j = 0; j < 20; j++) {
            long btime = 0, dtime = 0, rtime = 0;
            for (int i = 0; i < count; i++) {
                long start = System.nanoTime();

                bytes.clear();
                bf1.writeMarshallable(bytes);
                bf2.readMarshallable(bytes);

                long mid1 = System.nanoTime();

                bytes.clear();
                rf1.writeMarshallable(bytes);
                rf2.readMarshallable(bytes);

                long mid2 = System.nanoTime();

                bytes.clear();
                df1.writeMarshallable(bytes);
                df2.readMarshallable(bytes);

                long end = System.nanoTime();

                btime += mid1 - start;
                rtime += mid2 - mid1;
                dtime += end - mid2;
            }
            btime /= count;
            dtime /= count;
            rtime /= count;
            double r_b = 100 * rtime / btime / 100.0;
            double d_b = 100 * dtime / btime / 100.0;
            if (0.7 <= r_b && r_b <= 0.98
                    && 0.55 <= d_b && d_b <= 0.7)
                break;
            System.out.println("btime: " + btime + ", rtime: " + rtime + ", dtime: " + dtime + ", r/b: " + r_b + ", d/b: " + d_b);
            if (j == 9)
                fail("btime: " + btime + ", rtime: " + rtime + ", dtime: " + dtime + ", r/b: " + r_b + ", d/b: " + d_b);
            Jvm.pause(j * 50);
        }
        bytes.releaseLast();
    }

}
