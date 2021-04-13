package net.openhft.chronicle.wire.bytesmarshallable;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.wire.BytesInBinaryMarshallable;
import net.openhft.compiler.CompilerUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.io.File;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.function.Predicate;
import java.util.stream.LongStream;

import static org.junit.Assert.fail;

public class PerfRegressionTest {

    final String cpuClass = Jvm.getCpuClass();

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
            a = read8Bit(bytes);
            b = read8Bit(bytes);
            c = read8Bit(bytes);
            d = read8Bit(bytes);
            e = read8Bit(bytes);
            f = read8Bit(bytes);
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
            write8Bit(bytes, e);
            write8Bit(bytes, f);
        }

        protected void write8Bit(BytesOut bytes, String a) {
            bytes.write8bit(a);
        }
    }

    @Test
    public void regressionTests() throws Exception {
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

        DefaultStringFields dsf0 = new DefaultStringFields(new String[6]);

        final Bytes direct = Bytes.allocateElasticDirect();
        final Bytes onHeap = Bytes.allocateElasticOnHeap();
        String file = IOTools.tempName("regressionTests");
        try (MappedBytes mapped = MappedBytes.mappedBytes(file, OS.pageSize())) {
            doTest("onHeap-default, direct-default, mapped-default, " +
                            "onHeap-default-string, direct-default-string, mapped-default-string, " +
                            "onHeap-null, direct-null, mapped-null," +
                            "onHeap-fields, direct-fields, mapped-fields, " +
                            "onHeap-reference, direct-reference, mapped-reference," +
                            "onHeap-string, direct-string, mapped-string," +
                            "",
                    times -> {
                        double od = times[0];
                        double dd = times[1];
                        double md = times[2];
                        double osd = times[3];
                        double dsd = times[4];
                        double msd = times[5];
                        return timesOk(od + dd + md, osd + dsd + msd, times[6] + times[7] + times[8]);
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
                        dsf0.writeMarshallable(onHeap);
                        for (int i = 0; i < 2; i++) {
                            onHeap.readPosition(0);
                            dsf2.readMarshallable(onHeap);
                        }
                    },
                    () -> {
                        direct.clear();
                        dsf0.writeMarshallable(direct);
                        for (int i = 0; i < 2; i++) {
                            direct.readPosition(0);
                            dsf2.readMarshallable(direct);
                        }
                    },
                    () -> {
                        mapped.clear();
                        dsf0.writeMarshallable(mapped);
                        for (int i = 0; i < 2; i++) {
                            mapped.readPosition(0);
                            dsf2.readMarshallable(mapped);
                        }
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

    private boolean timesOk(double d, double ds, double dn) {
        System.out.printf("PerfRegressionTest d: %.2f,  ds: %.2f, dn: %.2f%n", d, ds, dn);
        // assume it's our primary build server
        if (cpuClass.equals("AMD Ryzen 5 3600 6-Core Processor")) {
            if ((1.4 <= d && d <= 1.7) &&
                    (2.3 <= ds && ds <= 2.7) &&
                    (1.4 <= dn && dn <= 1.75))
                return true;

        } else if (cpuClass.startsWith("ARM")) {
            if ((1.6 <= d && d <= 2.0) &&
                    (3.6 <= ds && ds <= 4.4) &&
                    (0.85 <= dn && dn <= 1.05))
                return true;

        } else if (cpuClass.contains(" Xeon ")) {
            if ((1.7 <= d && d <= 2.2) &&
                    (2.5 <= ds && ds <= 3.1) &&
                    (1.3 <= dn && dn <= 1.6))
                return true;

        } else if (cpuClass.contains(" i7-10710U ")) {
            return ((1.4 <= d && d <= 1.7) &&
                    (2.3 <= ds && ds <= 2.7) &&
                    (1.4 <= dn && dn <= 1.75));
        }
        throw new UnsupportedOperationException();
    }

    void doTest(String names, Predicate<double[]> check, Runnable... tests) throws Exception {
        long[] times = new long[tests.length];
        int count = 250_000;
        int repeats = 10, outlier = Jvm.isArm() ? 200_000 : 10_000;
        String[] namesArr = names.split(", ?");
        String className = "Runnable" + Long.toString(System.nanoTime(), 36);
        String code = "public class " + className + " implements Runnable {\n" +
                "long[] times;\n" +
                "Runnable[] tests;\n" +
                "public " + className + "(long[] times, Runnable[] tests) { this.times = times; this.tests = tests; }\n" +
                "public void run() {";
        for (int t = 0; t < tests.length; t++) {
            code += "{\n" +
                    "   long start = System.nanoTime();\n" +
                    "    tests[" + t + "].run();\n" +
                    "    long end = System.nanoTime();\n" +
                    "    times[" + t + "] += Math.min(" + outlier + ", end - start);\n" +
                    "}\n";
        }
        code += "    }\n" +
                "}";
        if (Jvm.getBoolean("dumpCode", false))
            System.out.println(code);
        Class clazz = CompilerUtils.CACHED_COMPILER.loadFromJava(className, code);
        Runnable runTests = (Runnable) clazz.getConstructors()[0].newInstance(times, tests);
        boolean fails = true;
        for (int j = 0; j <= repeats; j++) {
            if (j == 0)
                count = 20_000;
            for (int i = 0; i < count; i++) {
                runTests.run();
            }
            final long[] longs = LongStream.of(times.clone()).sorted().toArray();
            long mid = (longs[times.length / 2 - 1] + longs[times.length / 2] + longs[times.length / 2 + 1]) / 3;
            //noinspection IntegerDivisionInFloatingPointContext
            double[] relTimes =
                    LongStream.of(times)
                            .mapToDouble(i -> 100L * i / mid / 100.0)
                            .toArray();
            System.out.print(cpuClass);
            for (int t = 0; t < namesArr.length; t++)
                System.out.print(", " + namesArr[t] + ": " + relTimes[t]);
            System.out.println();
            try {
                if (check.test(relTimes))
                    return;
            } catch (UnsupportedOperationException e) {
                fails = false;
            }
            Jvm.pause(j * 50L);
        }
        if (fails)
            fail("Performance outside range");
    }
}
