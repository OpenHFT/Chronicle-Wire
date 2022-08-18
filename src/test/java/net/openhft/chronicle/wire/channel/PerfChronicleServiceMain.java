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

package net.openhft.chronicle.wire.channel;

import net.openhft.affinity.AffinityLock;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.jlbh.JLBH;
import net.openhft.chronicle.jlbh.JLBHOptions;
import net.openhft.chronicle.jlbh.JLBHTask;
import net.openhft.chronicle.wire.channel.echo.DummyData;
import net.openhft.chronicle.wire.channel.echo.EchoHandler;

import java.util.concurrent.locks.LockSupport;

/*
** Ryzen 9 5950X with Ubuntu 21.10 run with **

-Durl=tcp://localhost:1248 -Dsize=256 -Dthroughput=100000 -Diterations=3000000 -Dbuffered=false
-------------------------------- SUMMARY (end to end) us -------------------------------------------
Percentile   run1         run2         run3         run4         run5      % Variation
50.0:            6.98         7.00         7.02         7.02         7.02         0.15
90.0:            7.06         7.06         7.06         7.08         7.10         0.30
99.0:            7.16         7.18         7.16         7.18         7.18         0.15
99.7:            7.94         8.12         8.01         8.15         8.14         1.18
99.9:            8.91         9.52         9.04         9.71         9.65         4.72
99.97:          10.48        12.53        10.96        14.61        14.58        18.16
99.99:          14.48        15.06        15.12        15.92        15.98         3.95
99.997:         15.31        16.86        16.93        18.46        18.66         6.62
worst:         217.34        86.14        53.06        22.62        22.62        65.18

----------------------------------------------------------------------------------------------------

-Durl=tcp://localhost:1248 -Dsize=256 -Dthroughput=1000000 -Diterations=30000000 -Dbuffered=true
-------------------------------- SUMMARY (end to end) us -------------------------------------------
Percentile   run1         run2         run3         run4         run5      % Variation
50.0:           16.54        16.54        16.54        16.61        16.61         0.26
90.0:           20.32        20.32        20.32        20.38        20.38         0.21
99.0:           22.88        22.94        23.01        23.01        23.01         0.19
99.7:           24.10        24.22        24.42        24.42        24.35         0.53
99.9:           26.08        26.08        26.27        26.21        26.14         0.49
99.97:          28.06        27.62        28.13        27.62        27.62         1.22
99.99:          29.54        28.32        28.51        28.26        28.26         0.60
99.997:        730.11       164.10       576.51        28.38        28.38        92.79
99.999:       1075.20       377.34       973.82        28.96        29.09        95.60
99.9997:      1185.79       391.68      1107.97        30.18        31.01        95.97
worst:        1239.04       430.59      1165.31        34.37        69.25        95.64

** Intel(R) Xeon(R) CPU E5-2650 v4 @ 2.20GHz **
-Durl=tcp://server-via-onload:1248  -Dsize=256 -Dthroughput=100000 -Diterations=3000000 -Dbuffered=false
-------------------------------- SUMMARY (end to end) us -------------------------------------------
Percentile   run1         run2         run3         run4         run5      % Variation
50.0:           16.74        16.74        16.74        16.74        16.74         0.00
90.0:           17.18        17.18        17.18        17.18        17.18         0.00
99.0:           18.21        18.21        18.21        18.21        18.21         0.00
99.7:           19.36        19.30        19.30        19.30        19.36         0.22
99.9:           20.26        20.13        20.13        20.19        20.26         0.42
99.97:          21.79        21.54        21.54        21.54        21.66         0.39
99.99:          25.44        24.35        24.67        25.31        25.25         2.56
99.997:         37.57        46.27        29.02        47.17        59.33        41.04
worst:         159.49       196.86       161.02       162.05       229.12        21.99

-Durl=tcp://server-via-onload:1248  -Dsize=256 -Dthroughput=1000000 -Diterations=30000000 -Dbuffered=true
-------------------------------- SUMMARY (end to end) us -------------------------------------------
Percentile   run1         run2         run3         run4         run5      % Variation
50.0:           35.01        34.75        34.62        34.50        34.50         0.49
90.0:           41.28        41.02        40.90        40.77        40.77         0.42
99.0:           47.42        46.91        46.78        46.53        46.66         0.55
99.7:           54.59        49.73        49.22        48.96        48.96         1.03
99.9:         3330.05        53.70        51.78        51.26        51.26         3.07
99.97:        4448.26        95.62        70.27        55.36        54.72        33.26
99.99:        5382.14       185.09       158.46       140.03       104.58        33.92
99.997:       6086.66       609.28       326.14       228.61       162.56        64.69
99.999:       6250.50       715.78       601.09       456.19       178.43        66.75
99.9997:      6381.57       746.50       699.39       553.98       186.11        66.75
worst:        6463.49       783.36       736.26       592.90       199.94        66.05
----------------------------------------------------------------------------------------------------
*/

/* for profiling
-XX:+UnlockCommercialFeatures
-XX:+FlightRecorder
-XX:+UnlockDiagnosticVMOptions
-XX:+DebugNonSafepoints
-XX:StartFlightRecording=filename=recording_echo.jfr,settings=profile
 */

public class PerfChronicleServiceMain implements JLBHTask {
    static final int THROUGHPUT = Integer.getInteger("throughput", 100_000);

    static final int BATCH = Integer.getInteger("batch", 1);
    static final int ITERATIONS = Integer.getInteger("iterations", THROUGHPUT * 30);
    static final int SIZE = Integer.getInteger("size", 256);
    static final boolean BUFFERED = Jvm.getBoolean("buffered");
    static final String URL = System.getProperty("url", "tcp://:1248");
    private DummyData data;
    private Echoing echoing;
    private MethodReader reader;
    private Thread readerThread;
    private ChronicleChannel client;
    private volatile boolean complete;
    private int sent;
    private volatile int count;
    private boolean warmedUp;
    private ChronicleContext context;

    public static void main(String[] args) {
        System.out.println("" +
                "-Durl=" + URL + " " +
                "-Dsize=" + SIZE + " " +
                "-Dbatch=" + BATCH + " " +
                "-Dthroughput=" + THROUGHPUT + " " +
                "-Diterations=" + ITERATIONS + " " +
                "-Dbuffered=" + BUFFERED);

        JLBHOptions lth = new JLBHOptions()
                .warmUpIterations(50_000)
                .iterations(ITERATIONS/BATCH)
                .throughput(THROUGHPUT/BATCH)
                .acquireLock(AffinityLock::acquireLock)
                // disable as otherwise single GC event skews results heavily
                .recordOSJitter(false)
                .accountForCoordinatedOmission(false)
                .runs(5)
                .jlbhTask(new PerfChronicleServiceMain());
        new JLBH(lth).start();
    }

    @Override
    public void init(JLBH jlbh) {
        this.data = new DummyData();
        this.data.data(new byte[SIZE - Long.BYTES]);

        context = ChronicleContext.newContext(URL);

        final EchoHandler handler = new EchoHandler()
                .buffered(BUFFERED);
        client = context.newChannelSupplier(handler).buffered(BUFFERED).get();
        echoing = client.methodWriter(Echoing.class);
        reader = client.methodReader((Echoing) data -> {
            if (data.data()[0] == 0) {
                jlbh.sample(System.nanoTime() - data.timeNS());
                count++;
            }
        });
        readerThread = new Thread(() -> {
            try (AffinityLock lock = AffinityLock.acquireLock()) {
                while (!Thread.currentThread().isInterrupted()) {
                    reader.readOne();
                }
            } catch (Throwable t) {
                if (!complete)
                    t.printStackTrace();
            }
        }, "last-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    @Override
    public void warmedUp() {
        JLBHTask.super.warmedUp();
        warmedUp = true;
    }

    @Override
    public void run(long startTimeNS) {
        data.timeNS(startTimeNS);
        data.data()[0] = 0;
        for (int b = 0; b < BATCH; b++) {
            echoing.echo(data);
            data.data()[0] = 1;
        }

        // throttling when warming up.
        if (!warmedUp) {
            long lag = sent++ - count;
            if (lag >= 50)
                LockSupport.parkNanos(lag * 500L);
        }
    }

    @Override
    public void complete() {
        this.complete = true;
        readerThread.interrupt();
        client.close();
    }
}
