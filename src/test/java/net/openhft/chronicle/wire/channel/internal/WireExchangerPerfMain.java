package net.openhft.chronicle.wire.channel.internal;

import net.openhft.affinity.AffinityLock;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.jlbh.JLBH;
import net.openhft.chronicle.jlbh.JLBHOptions;
import net.openhft.chronicle.jlbh.JLBHTask;
import net.openhft.chronicle.wire.channel.impl.WireExchanger;

import java.nio.ByteBuffer;

/*
-------------------------------- SUMMARY (end to end) us -------------------------------------------
Percentile   run1         run2         run3         run4         run5      % Variation
50.0:            0.19         0.19         0.20         0.20         0.20         3.39
90.0:            0.21         0.21         0.21         0.21         0.21         0.00
99.0:            0.21         0.21         0.21         0.21         0.21         0.00
99.7:            0.22         0.23         0.21         0.21         0.21         5.96
99.9:            0.46         0.48         0.51         0.50         0.50         6.75
99.97:           0.77         0.77         0.78         0.77         0.77         0.86
99.99:           1.01         0.99         0.99         0.98         0.98         2.00
99.997:          1.38         1.35         1.26         1.30         1.28         5.96
99.999:          1.82         1.61         1.49         1.46         1.49        14.10
worst:          19.17        22.05         2.97         4.18         3.57        81.06
-------------------------------------------------------------------------------------------------------------------
On Windows, (without affinity)
Percentile   run1         run2         run3         run4         run5      % Variation
50.0:            0.10         0.10         0.10         0.10         0.10         0.00
90.0:            0.10         0.10         0.10         0.10         0.10         0.00
99.0:            0.10         0.10         0.10         0.10         0.10         0.00
99.7:            0.20         0.10         0.10         0.10         0.10        40.00
99.9:            2.00         0.80         0.60         1.30         2.10        62.45
99.97:          16.30        15.22        14.80        15.50        15.70         6.34
99.99:          26.91        25.31        25.31        24.80        24.67         5.71
99.997:         41.28        36.16        38.21        36.03        36.16         8.85
99.999:         88.45        47.55        67.97        52.03        53.82        36.44
worst:         295.42        96.13       222.46       179.46       187.14        58.02

 */

public class WireExchangerPerfMain implements JLBHTask {

    private static final int warmup = 500_000;
    private static final int iterations = 10_000_000;
    private static final int throughput = 500_000;
    private final WireExchanger be = new WireExchanger();
    private int count = 0;
    private JLBH jlbh;
    private volatile boolean started;

    public static void main(String[] args) {
        JLBHOptions lth = new JLBHOptions()
                .warmUpIterations(warmup)
                .iterations(iterations)
                .throughput(throughput)
                .acquireLock(AffinityLock::acquireLock)
                // disable as otherwise single GC event skews results heavily
                .recordOSJitter(false)
                .accountForCoordinatedOmission(true)
                .skipFirstRun(false)
                .runs(5)
                .jlbhTask(new WireExchangerPerfMain());
        new JLBH(lth).start();
    }

    @Override
    public void init(JLBH jlbh) {
        this.jlbh = jlbh;
        final Thread consumer = new Thread(this::run, "consumer");
        consumer.setDaemon(true);
        consumer.start();
    }

    private void run() {
        try (AffinityLock lock = AffinityLock.acquireLock()) {
            started = true;
            while (!Thread.currentThread().isInterrupted()) {
                final Bytes<ByteBuffer> bytes = (Bytes<ByteBuffer>) be.acquireConsumer().bytes();
                while (bytes.readRemaining() > 0) {
                    long time = bytes.readLong();
                    jlbh.sample(System.nanoTime() - time);
                }
                bytes.clear();
                be.releaseConsumer();
            }
        }
    }

    @Override
    public void run(long startTimeNS) {
        final Bytes<ByteBuffer> bytes = (Bytes<ByteBuffer>) be.acquireProducer().bytes();
        if (bytes.writePosition() > 32000) {
            System.out.print(".");
            Jvm.pause(++count);
        }

        bytes.writeLong(startTimeNS);
        be.releaseProducer();
    }
}
