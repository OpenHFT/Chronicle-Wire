/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.bytesmarshallable;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.WireTestCommon;
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

public class PerfRegressionTest extends WireTestCommon {

    final String cpuClass = Jvm.getCpuClass();  // Likely obtaining some CPU class information from a utility class 'Jvm'.

    @Ignore("Long running")
    @Test
    public void regressionTests() throws Exception {
        final URL location = PerfRegressionTest.class.getProtectionDomain().getCodeSource().getLocation();
//        System.getProperties().forEach((k,v) -> System.out.println(k+"= "+v));
        File file = new File(location.getFile());

        // Navigate the directory structure to the "target" directory.
        do {
            file = file.getParentFile();
        } while (!file.getName().equals("target"));

        // Array of classes that appear to be subjected to the benchmark test.
        Class[] classes = {
//                BenchBytesMain.class,
                BenchStringMain.class,
                BenchArrayStringMain.class,
//                BenchNullMain.class,
//                BenchFieldsMain.class,
//                BenchRefBytesMain.class,
                BenchRefStringMain.class,
                BenchUtf8StringMain.class,
        };

        // 'runs' determines the number of times the benchmark will be run for each class.
        int runs = 5;
        long[][] times = new long[classes.length][runs];

        // Launch processes and collect execution times.
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

        // Sort captured times for each class.
        for (long[] time : times) {
            Arrays.sort(time);
        }

        // Calculate and output median execution times and their sum.
        double sum = 0;
        for (int i = 0; i < classes.length; i++) {
            Class aClass = classes[i];
            final long[] time = times[i];
            final long time2 = time[time.length / 2];  // Median time value
            sum += time2;
            System.out.println(aClass.getSimpleName() + " = " + time2);
        }
        sum /= classes.length;  // Average of the median times

        // Further calculations and performance verification
        final double d = times[0][1] / sum;
        final double ds = times[1][1] / sum;
        final double dn = times[2][1] / sum;

        // Outputting and checking the performance values.
        String msg = String.format("PerfRegressionTest d: %.2f,  ds: %.2f, dn: %.2f%n", d, ds, dn);
        System.out.println(msg);

        // Verify whether the captured performance metrics are within acceptable ranges.
        try {
            boolean ok = timesOk(d, ds, dn);  // timesOk() is not defined in the snippet.
            if (!ok)
                fail("Outside performance range " + msg);  // Failing the test if metrics are outside acceptable ranges.
        } catch (UnsupportedOperationException ignored) {
            System.err.println("Outside performance range " + msg);
        }
    }

    // Create and start a new process for executing a specified class in a Maven environment
    @NotNull
    private Process getProcess(File file, Class aClass) throws IOException {
        // Initialize ProcessBuilder with Maven command and specify main class to be executed
        ProcessBuilder pb = new ProcessBuilder("mvn", "exec:java",
                "-Dexec.classpathScope=test",
                "-Dexec.mainClass=" + aClass.getName());

        // Merge the process's error stream into the standard output stream
        pb.redirectErrorStream(true);

        // Set JAVA_HOME environment variable ensuring it points to a JDK and not a JRE
        pb.environment().put("JAVA_HOME", System.getProperty("java.home").replace("jre", ""));

        // Set the working directory for the process to the parent of the provided file
        pb.directory(file.getParentFile());

        // Start the process and return it
        final Process process = pb.start();
        return process;
    }

    // Retrieve and return the execution result from the output stream of a given process
    private long getResult(Class aClass, long result, Process process) throws IOException, InterruptedException {
        // Wrap the process's input stream with a BufferedReader to read its output
        try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            // Iterate through each line of the process output
            for (String line; (line = br.readLine()) != null; ) {
                // If a line starts with "result:", extract and update the result value
                if (line.startsWith("result:")) {
                    System.out.println(aClass.getSimpleName() + " - " + line);
                    result = Long.parseLong(line.split(" ")[1]);
                }

            }
        }

        // Wait for the process to terminate and retrieve its exit value
        int ret = process.waitFor();
        // Log a message if the process does not terminate successfully
        if (ret != 0)
            System.out.println("Returned " + ret);

        // Ensure the process is terminated
        process.destroy();
        return result;
    }
//            doTest(
//                    times -> timesOk(times[0], times[1], times[2]));
//        }

    // Determine if execution times for a set of tests are within acceptable ranges
    private boolean timesOk(double d, double ds, double dn) {
        // Validate times against predefined thresholds depending on the CPU class
        if (cpuClass.equals("AMD Ryzen 5 3600 6-Core Processor") ||
            cpuClass.startsWith("ARM") ||
            cpuClass.contains(" Xeon")) {

            // Check if times are within the defined ranges for general CPUs
            if ((0.7 <= d && d <= 0.92) &&
                    (0.72 <= ds && ds <= 0.82) &&
                    (0.74 <= dn && dn <= 0.9))
                return true;

        } else if (cpuClass.contains(" i7-10710U ")) {
            // Check if times are within the defined ranges for i7-10710U CPUs
            return ((0.59 <= d && d <= 0.66) &&
                    (0.89 <= ds && ds <= 0.95) &&
                    (0.80 <= dn && dn <= 0.92));
        }
        // If none of the conditions were met, unsupported operation
        throw new UnsupportedOperationException();
    }
}
