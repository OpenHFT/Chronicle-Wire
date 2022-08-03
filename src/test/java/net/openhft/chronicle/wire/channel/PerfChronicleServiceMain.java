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
Ryzen 9 5950X with Ubuntu 21.10 run with

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

-Durl=tcp://localhost:65432 -Dsize=256 -Dthroughput=1000000 -Diterations=30000000 -Dbuffered=true
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
----------------------------------------------------------------------------------------------------



-XX:+UnlockCommercialFeatures
-XX:+FlightRecorder
-XX:+UnlockDiagnosticVMOptions
-XX:+DebugNonSafepoints
-XX:StartFlightRecording=filename=recording_echo.jfr,settings=profile
 */

public class PerfChronicleServiceMain implements JLBHTask {
    static final int THROUGHPUT = Integer.getInteger("throughput", 100_000);
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
                "-Dthroughput=" + THROUGHPUT + " " +
                "-Diterations=" + ITERATIONS + " " +
                "-Dbuffered=" + BUFFERED);

        JLBHOptions lth = new JLBHOptions()
                .warmUpIterations(50_000)
                .iterations(ITERATIONS)
                .throughput(THROUGHPUT)
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
            jlbh.sample(System.nanoTime() - data.timeNS());
            count++;
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
        echoing.echo(data);

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
