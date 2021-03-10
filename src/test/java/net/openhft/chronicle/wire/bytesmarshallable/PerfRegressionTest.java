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
import java.util.function.BooleanSupplier;

public class PerfRegressionTest {

    final String cpuClass = Jvm.getCpuClass();
    private double b_r;
    private double d_r;

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
    public void bytesPerformanceDirect() {
        final Bytes bytes = Bytes.allocateElasticDirect();
        doRegressionTest("direct", bytes, this::directOk);
    }

    private boolean directOk() {
        // assume it's our primary build server
        if (cpuClass.equals("AMD Ryzen 5 3600 6-Core Processor")) {
            boolean brOk = 0.81 <= b_r && b_r <= 0.87;
            boolean drOk = 0.35 <= d_r && d_r <= 0.38;
            if (Jvm.isJava9Plus()) {
                brOk = 0.64 <= b_r && b_r <= 0.68;
                drOk = 0.48 <= d_r && d_r <= 0.52;
            }
            return brOk && drOk;

        } else if (cpuClass.startsWith("ARMv7")) {
            return true;

        } else if (cpuClass.contains("  E3-1270 v3 ")) {
            boolean brOk = 1.17 <= b_r && b_r <= 1.25;
            boolean drOk = 0.34 <= d_r && d_r <= 0.38;
//            if (Jvm.isJava9Plus())
//                drOk = 0.4 <= d_r && d_r <= 0.66;
            return brOk && drOk;

        } else if (cpuClass.contains("  E5-2650 v4 ")) {
            boolean brOk = 1.08 <= b_r && b_r <= 1.11;
            boolean drOk = 0.49 <= d_r && d_r <= 0.55;
//            if (Jvm.isJava9Plus())
//                drOk = 0.4 <= d_r && d_r <= 0.66;
            return brOk && drOk;

        } else if (cpuClass.contains(" i7-10710U ")) {
            boolean brOk = 0.68 <= b_r && b_r <= 0.72;
            if (Jvm.isJava9Plus())
                brOk = 0.71 <= b_r && b_r <= 0.73;
            if (brOk
                    && 0.48 <= d_r && d_r <= 0.52)
                return true;

        } else {
            boolean brOk = 0.65 <= b_r && b_r <= 0.87;
            if (Jvm.isJava9Plus())
                brOk = 0.7 <= b_r && b_r <= 0.98;
            if (brOk
                    && 0.39 <= d_r && d_r <= 0.61)
                return true;
        }
        return false;
    }

    @Test
    public void bytesPerformanceOnHeap() {
        final Bytes bytes = Bytes.allocateElasticOnHeap();
        doRegressionTest("onHeap", bytes, this::onHeapOk);
    }


    private boolean onHeapOk() {
        // assume it's our primary build server
        if (cpuClass.equals("AMD Ryzen 5 3600 6-Core Processor")) {
            boolean brOk = 0.71 <= b_r && b_r <= 0.78;
            boolean drOk = 0.42 <= d_r && d_r <= 0.52;
            if (Jvm.isJava9Plus()) {
                brOk = 0.92 <= b_r && b_r <= 0.99;
                drOk = 0.62 <= d_r && d_r <= 0.65;
            }
            return brOk && drOk;

        } else if (cpuClass.startsWith("ARMv7")) {
            return true;

        } else if (cpuClass.contains("  E3-1270 v3 ")) {
            boolean brOk = 0.83 <= b_r && b_r <= 0.89;
            boolean drOk = 0.61 <= d_r && d_r <= 0.69;
//            if (Jvm.isJava9Plus())
//                drOk = 0.4 <= d_r && d_r <= 0.66;
            return brOk && drOk;

        } else if (cpuClass.contains("  E5-2650 v4 ")) {
            boolean brOk = 0.80 <= b_r && b_r <= 0.86;
            boolean drOk = 0.66 <= d_r && d_r <= 0.69;
//            if (Jvm.isJava9Plus())
//                drOk = 0.4 <= d_r && d_r <= 0.66;
            return brOk && drOk;

        } else if (cpuClass.contains(" i7-10710U ")) {
            boolean brOk = 0.7 <= b_r && b_r <= 0.77;
            boolean drOk = 0.55 <= d_r && d_r <= 0.59;
            if (Jvm.isJava9Plus())
                drOk = 0.62 <= d_r && d_r <= 0.66;
            return brOk && drOk;

        } else {
            boolean brOk = 0.65 <= b_r && b_r <= 0.87;
            if (Jvm.isJava9Plus())
                brOk = 0.7 <= b_r && b_r <= 0.98;
            if (brOk
                    && 0.39 <= d_r && d_r <= 0.61)
                return true;
        }
        return false;
    }

    private void doRegressionTest(String desc, Bytes bytes, BooleanSupplier test) {
        BytesFields bf1 = new BytesFields("12", "12345", "123456789012", "12345678901234567890123");
        BytesFields bf2 = new BytesFields();

        DefaultBytesFields df1 = new DefaultBytesFields("12", "12345", "123456789012", "12345678901234567890123");
        DefaultBytesFields df2 = new DefaultBytesFields();

        ReferenceBytesFields rf1 = new ReferenceBytesFields("12", "12345", "123456789012", "12345678901234567890123");
        ReferenceBytesFields rf2 = new ReferenceBytesFields();

        int count = 250_000;
        int repeats = 10, outlier = 10_000;
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

                btime += Math.min(outlier, mid1 - start);
                rtime += Math.min(outlier, mid2 - mid1);
                dtime += Math.min(outlier, end - mid2);
            }
            btime /= count;
            dtime /= count;
            rtime /= count;
            b_r = 100 * btime / rtime / 100.0;
            d_r = 100 * dtime / rtime / 100.0;
            if (j == 0) {
                Thread.yield();
                continue;
            }
            final String msg = cpuClass + " - " + desc + " - btime: " + btime + ", rtime: " + rtime + ", dtime: " + dtime + ", b/r: " + b_r + ", d/r: " + d_r;
            System.out.println(msg);
            if (test.getAsBoolean())
                break;
            if (j == repeats) {
                System.out.println("FAIL");
                // fail(msg);
            }
            Jvm.pause(j * 50L);
        }
        bytes.releaseLast();
    }

}
