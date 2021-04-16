package net.openhft.chronicle.wire.bytesmarshallable;

import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;

import static org.junit.Assert.fail;

public class PerfRegressionTest {

    final String cpuClass = Jvm.getCpuClass();

    @Ignore("Long running")
    @Test
    public void regressionTests() throws Exception {
        final URL location = PerfRegressionTest.class.getProtectionDomain().getCodeSource().getLocation();
//        System.getProperties().forEach((k,v) -> System.out.println(k+"= "+v));
        File file = new File(location.getFile());
        do {
            file = file.getParentFile();
        } while (!file.getName().equals("target"));

        Class[] classes = {
//                BenchBytesMain.class,
                BenchStringMain.class,
//                BenchNullMain.class,
//                BenchFieldsMain.class,
//                BenchRefBytesMain.class,
                BenchRefStringMain.class,
                BenchUtf8StringMain.class,
        };
        int runs = 5;
        long[][] times = new long[classes.length][runs];
        for (int r = 0; r < runs; r++) {
            Process[] processes = new Process[classes.length];
            int prev = -1;
            for (int i = classes.length - 1; i >= 0; i--) {
                Class aClass = classes[i];
                processes[i] = getProcess(file, aClass);
                if (prev > -1) {
                    times[prev][r] = getResult(classes[prev], Long.MAX_VALUE, processes[prev]);
                }
                prev = i;
            }
            times[prev][r] = getResult(classes[prev], Long.MAX_VALUE, processes[prev]);
        }
        for (long[] time : times) {
            Arrays.sort(time);
        }
        double sum = 0;
        for (int i = 0; i < classes.length; i++) {
            Class aClass = classes[i];
            final long[] time = times[i];
            final long time2 = time[time.length / 2];
            sum += time2;
            System.out.println(aClass.getSimpleName() + " = " + time2);
        }
        sum /= classes.length;
        final double d = times[0][1] / sum;
        final double ds = times[1][1] / sum;
        final double dn = times[2][1] / sum;
        String msg = String.format("PerfRegressionTest d: %.2f,  ds: %.2f, dn: %.2f%n", d, ds, dn);
        System.out.println(msg);
        try {
            boolean ok = timesOk(d, ds, dn);
            if (!ok)
                fail("Outside performance range " + msg);
        } catch (UnsupportedOperationException ignored) {
            System.err.println("Outside performance range " + msg);
        }
    }

    @NotNull
    private Process getProcess(File file, Class aClass) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("mvn", "exec:java",
                "-Dexec.classpathScope=test",
                "-Dexec.mainClass=" + aClass.getName());
        pb.redirectErrorStream(true);
        pb.environment().put("JAVA_HOME", System.getProperty("java.home").replace("jre", ""));
        pb.directory(file.getParentFile());
        final Process process = pb.start();
        return process;
    }

    private long getResult(Class aClass, long result, Process process) throws IOException, InterruptedException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            for (String line; (line = br.readLine()) != null; ) {
                if (line.startsWith("result:")) {
                    System.out.println(aClass.getSimpleName() + " - " + line);
                    result = Long.parseLong(line.split(" ")[1]);
                }

            }
        }
        int ret = process.waitFor();
        if (ret != 0)
            System.out.println("Returned " + ret);
        process.destroy();
        return result;
    }
//            doTest(
//                    times -> timesOk(times[0], times[1], times[2]));
//        }


    private boolean timesOk(double d, double ds, double dn) {
        // assume it's our primary build server
        if (cpuClass.equals("AMD Ryzen 5 3600 6-Core Processor")) {
            if ((0.7 <= d && d <= 0.92) &&
                    (0.72 <= ds && ds <= 0.82) &&
                    (0.74 <= dn && dn <= 0.9))
                return true;

        } else if (cpuClass.startsWith("ARM")) {
            if ((0.7 <= d && d <= 0.92) &&
                    (0.72 <= ds && ds <= 0.82) &&
                    (0.74 <= dn && dn <= 0.9))
                return true;

        } else if (cpuClass.contains(" Xeon")) {
            if ((0.7 <= d && d <= 0.92) &&
                    (0.72 <= ds && ds <= 0.82) &&
                    (0.74 <= dn && dn <= 0.9))
                return true;

        } else if (cpuClass.contains(" i7-10710U ")) {
            return ((0.59 <= d && d <= 0.66) &&
                    (0.89 <= ds && ds <= 0.95) &&
                    (0.80 <= dn && dn <= 0.92));
        }
        throw new UnsupportedOperationException();
    }
}
