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

-Durl=tcp://localhost:65432 -Dsize=256 -Dthroughput=100000 -Diterations=3000000 -Dbuffered=false
-------------------------------- SUMMARY (end to end) us -------------------------------------------
Percentile   run1         run2         run3         run4         run5      % Variation
50.0:           33.86        33.60        33.86        33.60        33.86         0.51
90.0:           35.65        34.24        35.52        34.24        35.52         2.43
99.0:           37.44        37.18        37.31        36.67        37.31         1.15
99.7:           39.10        38.46        38.46        37.95        38.59         1.11
99.9:           44.99        41.15        39.23        38.85        39.36         3.80
99.97:          46.27        45.12        40.38        39.74        41.02         8.27
99.99:        3108.86        46.14        45.12        41.66        53.95        16.43
worst:       47382.53     43450.37     47382.53     47382.53     48693.25         7.45


----------------------------------------------------------------------------------------------------

-Durl=tcp://localhost:65432 -Dsize=256 -Dthroughput=1000000 -Diterations=30000000 -Dbuffered=true
-------------------------------- SUMMARY (end to end) us -------------------------------------------
Percentile   run1         run2         run3         run4         run5      % Variation
50.0:           19.04        19.04        19.04        19.10        19.10         0.22
90.0:           21.66        21.60        21.66        21.66        21.66         0.20
99.0:           23.39        23.26        23.33        23.33        23.33         0.18
99.7:           24.03        23.97        24.03        24.03        24.03         0.18
99.9:           25.63        25.63        25.57        25.63        25.63         0.17
99.97:          26.98        27.04        26.91        26.98        26.98         0.32
99.99:          27.94       740.35        27.10        27.10        27.10        94.61
99.997:        279.04      1492.99        27.42        27.42        27.62        97.27
99.999:        345.60      1648.64        28.19        28.19        28.51        97.46
99.9997:      1148.93      1701.89        30.24        29.15        30.24        97.45
worst:        1398.78      1734.66        54.59        35.26        64.70        96.98


 */

public class PerfEchoHandlerMain implements JLBHTask {
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
                .jlbhTask(new PerfEchoHandlerMain());
        new JLBH(lth).start();
    }

    @Override
    public void init(JLBH jlbh) {
        this.data = new DummyData();
        this.data.data(new byte[SIZE - Long.BYTES]);

        context = ChronicleContext.newContext(URL);

        final EchoHandler echoHandler = new EchoHandler();
        client = context.newChannelSupplier(echoHandler).buffered(BUFFERED).get();
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
        context.close();
    }
}
