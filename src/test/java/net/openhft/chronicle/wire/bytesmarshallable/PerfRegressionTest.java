package net.openhft.chronicle.wire.bytesmarshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.BytesInBinaryMarshallable;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static org.junit.Assert.fail;

@Ignore("https://github.com/OpenHFT/Chronicle-Wire/issues/267")
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
        BytesFields bf1 = new BytesFields("12", "12345", "123456789012", "12345678901234567890123");
        BytesFields bf2 = new BytesFields();

        DefaultBytesFields df1 = new DefaultBytesFields("12", "12345", "123456789012", "12345678901234567890123");
        DefaultBytesFields df2 = new DefaultBytesFields();

        ReferenceBytesFields rf1 = new ReferenceBytesFields("12", "12345", "123456789012", "12345678901234567890123");
        ReferenceBytesFields rf2 = new ReferenceBytesFields();

        final Bytes bytes = Bytes.allocateElasticDirect();
        int count = 250_000;
        int repeats = 5;
        final String cpuClass = Jvm.getCpuClass();
        for (int j = 0; j <= repeats; j++) {
            long btime = 0, dtime = 0, rtime = 0;
            if (j == 0)
                count = 20_000;
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
            double b_r = 100 * btime / rtime / 100.0;
            double d_r = 100 * dtime / rtime / 100.0;
            if (j == 0) {
                Thread.yield();
                continue;
            }
            // assume it's our primary build server
            if (cpuClass.equals("AMD Ryzen 5 3600 6-Core Processor")) {
                if (0.8 <= b_r && b_r <= 0.82
                        && 0.50 <= d_r && d_r <= 0.52)
                    break;

            } else if (cpuClass.startsWith("ARMv7")) {
                if (0.5 <= b_r && b_r <= 0.6
                        && 0.12 <= d_r && d_r <= 0.2)
                    break;
            } else {
                boolean brOk = 0.65 <= b_r && b_r <= 0.87;
                if (Jvm.isJava9Plus())
                    brOk = 0.7 <= b_r && b_r <= 0.98;
                if (cpuClass.contains("CPU E3-1") && cpuClass.startsWith("AMD Ryzen 5"))
                    brOk = 0.9 <= b_r && b_r <= 1.1;
                if (brOk
                        && 0.39 <= d_r && d_r <= 0.61)
                    break;
            }
            System.out.println("btime: " + btime + ", rtime: " + rtime + ", dtime: " + dtime + ", b/r: " + b_r + ", d/b: " + d_r);
            if (j == repeats) {
                fail(cpuClass + " - btime: " + btime + ", rtime: " + rtime + ", dtime: " + dtime + ", b/r: " + b_r + ", d/b: " + d_r);
            }
            Jvm.pause(j * 50L);
        }
        bytes.releaseLast();
    }

}
