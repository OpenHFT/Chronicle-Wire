package net.openhft.chronicle.wire.bytesmarshallable;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.wire.BytesInBinaryMarshallable;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.function.Predicate;
import java.util.stream.LongStream;

public class PerfRegressionTest {

    final String cpuClass = Jvm.getCpuClass();

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

    static class StringFields extends BytesInBinaryMarshallable {
        String a = "";
        String b = "";
        String c = "";
        String d = "";

        public StringFields() {
        }

        public StringFields(String a, String b, String c, String d) {
            this();
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }
    }

    static class DefaultStringFields extends StringFields {
        public DefaultStringFields() {
        }

        public DefaultStringFields(String a, String b, String c, String d) {
            super(a, b, c, d);
        }

        @Override
        public void readMarshallable(BytesIn bytes) throws IORuntimeException, BufferUnderflowException, IllegalStateException {
            a = read8Bit(bytes);
            b = read8Bit(bytes);
            c = read8Bit(bytes);
            d = read8Bit(bytes);
        }

        protected @Nullable String read8Bit(BytesIn bytes) {
            return bytes.read8bit();
        }

        @Override
        public void writeMarshallable(BytesOut bytes) throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException {
            write8Bit(bytes, a);
            write8Bit(bytes, b);
            write8Bit(bytes, c);
            write8Bit(bytes, d);
        }

        protected void write8Bit(BytesOut bytes, String a) {
            bytes.write8bit(a);
        }
    }

    @Test
    public void regressionTests() throws FileNotFoundException {
        BytesFields bf1 = new BytesFields("12", "12345", "123456789012", "12345678901234567890123");
        BytesFields bf2 = new BytesFields();

        DefaultBytesFields df1 = new DefaultBytesFields("12", "12345", "123456789012", "12345678901234567890123");
        DefaultBytesFields df2 = new DefaultBytesFields();

        ReferenceBytesFields rf1 = new ReferenceBytesFields("12", "12345", "123456789012", "12345678901234567890123");
        ReferenceBytesFields rf2 = new ReferenceBytesFields();

        StringFields sf1 = new StringFields("12", "12345", "123456789012", "12345678901234567890123");
        StringFields sf2 = new StringFields();

        DefaultStringFields dsf1 = new DefaultStringFields("12", "12345", "123456789012", "12345678901234567890123");
        DefaultStringFields dsf2 = new DefaultStringFields();

        final Bytes direct = Bytes.allocateElasticDirect();
        final Bytes onHeap = Bytes.allocateElasticOnHeap();
        String file = IOTools.tempName("regressionTests");
        try (MappedBytes mapped = MappedBytes.mappedBytes(file, OS.pageSize())) {
            doTest("onHeap-default, direct-default, mapped-default, " +
                            "onHeap-default-string, direct-default-string, mapped-default-string, " +
                            "onHeap-fields, direct-fields, mapped-fields, " +
                            "onHeap-reference, direct-reference, mapped-reference," +
                            "onHeap-string, direct-string, mapped-string",
                    times -> {
                        double od = times[0];
                        double dd = times[1];
                        double md = times[2];
                        double osd = times[3];
                        double dsd = times[4];
                        double msd = times[5];
                        return timesOk(od, dd, md, osd, dsd, msd);
                    },
                    () -> {
                        onHeap.clear();
                        df1.writeMarshallable(onHeap);
                        df2.readMarshallable(onHeap);
                    },
                    () -> {
                        direct.clear();
                        df1.writeMarshallable(direct);
                        df2.readMarshallable(direct);
                    },
                    () -> {
                        mapped.clear();
                        df1.writeMarshallable(mapped);
                        df2.readMarshallable(mapped);
                    },
                    () -> {
                        onHeap.clear();
                        dsf1.writeMarshallable(onHeap);
                        dsf2.readMarshallable(onHeap);
                    },
                    () -> {
                        direct.clear();
                        dsf1.writeMarshallable(direct);
                        dsf2.readMarshallable(direct);
                    },
                    () -> {
                        mapped.clear();
                        dsf1.writeMarshallable(mapped);
                        dsf2.readMarshallable(mapped);
                    },
                    () -> {
                        onHeap.clear();
                        bf1.writeMarshallable(onHeap);
                        bf2.readMarshallable(onHeap);
                    },
                    () -> {
                        direct.clear();
                        bf1.writeMarshallable(direct);
                        bf2.readMarshallable(direct);
                    },
                    () -> {
                        mapped.clear();
                        bf1.writeMarshallable(mapped);
                        bf2.readMarshallable(mapped);
                    },
                    () -> {
                        onHeap.clear();
                        rf1.writeMarshallable(onHeap);
                        rf2.readMarshallable(onHeap);
                    },
                    () -> {
                        direct.clear();
                        rf1.writeMarshallable(direct);
                        rf2.readMarshallable(direct);
                    },
                    () -> {
                        mapped.clear();
                        rf1.writeMarshallable(mapped);
                        rf2.readMarshallable(mapped);
                    },
                    () -> {
                        onHeap.clear();
                        sf1.writeMarshallable(onHeap);
                        sf2.readMarshallable(onHeap);
                    },
                    () -> {
                        direct.clear();
                        sf1.writeMarshallable(direct);
                        sf2.readMarshallable(direct);
                    },
                    () -> {
                        mapped.clear();
                        sf1.writeMarshallable(mapped);
                        sf2.readMarshallable(mapped);
                    });
        }
        new File(file).delete();
        onHeap.releaseLast();
        direct.releaseLast();
    }

    private boolean timesOk(double od, double dd, double md, double osd, double dsd, double msd) {
        // assume it's our primary build server
        if (cpuClass.equals("AMD Ryzen 5 3600 6-Core Processor")) {
            if (Jvm.isJava9Plus())
                return (0.66 <= od && od <= 1) // TODO FIX OnHeap so it is more reliably optimised
                        && (0.39 <= dd && dd <= 0.49)
                        && (0.49 <= md && md <= 0.59)
                        && (0.59 <= osd && osd <= 1.02) // TODO FIX OnHeap so it is more reliably optimised
                        && (0.39 <= dsd && dsd <= 0.50)
                        && (0.40 <= msd && msd <= 0.47);
            return (0.85 <= od && od <= 1) // TODO FIX OnHeap so it is more reliably optimised
                    && (0.50 <= dd && dd <= 0.57)
                    && (0.62 <= md && md <= 0.7)
                    && (0.77 <= osd && osd <= 1.02) // TODO FIX OnHeap so it is more reliably optimised
                    && (0.44 <= dsd && dsd <= 0.53)
                    && (0.45 <= msd && msd <= 0.54);

        } else if (cpuClass.startsWith("ARMv7")) {

        } else if (cpuClass.contains(" Xeon ")) {
            return (0.84 <= od && od <= 1) // TODO FIX OnHeap so it is more reliably optimised
                    && (0.50 <= dd && dd <= 0.56)
                    && (0.68 <= md && md <= 0.75)
                    && (0.68 <= osd && osd <= 1.02) // TODO FIX OnHeap so it is more reliably optimised
                    && (0.44 <= dsd && dsd <= 0.53)
                    && (0.48 <= msd && msd <= 0.56);

        } else if (cpuClass.contains(" i7-10710U ")) {
            return (0.47 <= od && od <= 1) // TODO FIX OnHeap so it is more reliably optimised
                    && (0.52 <= dd && dd <= 0.64)
                    && (0.55 <= md && md <= 0.67)
                    && (0.46 <= osd && osd <= 1.02) // TODO FIX OnHeap so it is more reliably optimised
                    && (0.44 <= dsd && dsd <= 0.53)
                    && (0.41 <= msd && msd <= 0.53);
        }
        return true;
    }

    void doTest(String names, Predicate<double[]> check, Runnable... tests) {
        long[] times = new long[tests.length];
        int count = 250_000;
        int repeats = 10, outlier = 10_000;
        String[] namesArr = names.split(", ?");
        for (int j = 0; j <= repeats; j++) {
            if (j == 0)
                count = 20_000;
            for (int i = 0; i < count; i++) {
                for (int t = 0; t < tests.length; t++) {
                    long start = System.nanoTime();
                    tests[t].run();
                    long end = System.nanoTime();
                    times[t] += Math.min(outlier, end - start);
                }
            }
            long mid = LongStream.of(times.clone()).sorted().toArray()[times.length / 2];
            //noinspection IntegerDivisionInFloatingPointContext
            double[] relTimes =
                    LongStream.of(times)
                            .mapToDouble(i -> 100L * i / mid / 100.0)
                            .toArray();
            System.out.print(cpuClass);
            for (int t = 0; t < namesArr.length; t++)
                System.out.print(", " + namesArr[t] + ": " + relTimes[t]);
            System.out.println();
            if (check.test(relTimes))
                break;
            Jvm.pause(j * 50L);
        }
    }
}
