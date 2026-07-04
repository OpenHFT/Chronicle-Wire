/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

import org.junit.After;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Concurrency contracts of the metrics core: {@link LatencyInstrument#record(long)} must
 * never wait on sink I/O, and a facade created concurrently with
 * {@link Metrics#install(MetricsBinding)} must observe the new binding.
 */
public class LatencyInstrumentConcurrencyTest {

    @After
    public void resetMetrics() {
        Metrics.resetForTesting();
    }

    @Test(timeout = 10_000)
    public void recordIsNotBlockedByASlowFlushSink() throws InterruptedException {
        final LatencyInstrument instrument = new LatencyInstrument("chronicle.test", "chronicle_test_latency_ns");
        instrument.record(1_000);

        final CountDownLatch inSink = new CountDownLatch(1);
        final CountDownLatch releaseSink = new CountDownLatch(1);
        final MetricsOut slowSink = new MetricsOut() {
            @Override
            public void counterMetric(CounterMetric metric) {
            }

            @Override
            public void gaugeMetric(GaugeMetric metric) {
            }

            @Override
            public void histogramMetric(HistogramMetric metric) {
                inSink.countDown();
                try {
                    releaseSink.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            @Override
            public void rateMetric(RateMetric metric) {
            }

            @Override
            public void pointEvent(PointEvent metric) {
            }
        };

        final Thread flusher = new Thread(() -> instrument.flushTo(slowSink, 1, 1), "flusher");
        flusher.start();
        try {
            assertTrue(inSink.await(5, java.util.concurrent.TimeUnit.SECONDS));
            // the flusher is parked INSIDE the sink write; record() must complete regardless -
            // if it still shared the flush monitor across the sink write, this would deadlock
            // until the test timeout
            instrument.record(2_000);
        } finally {
            releaseSink.countDown();
            flusher.join();
        }
    }

    @Test(timeout = 30_000)
    public void facadeCreatedConcurrentlyWithInstallObservesTheNewBinding() throws Exception {
        for (int iter = 0; iter < 300; iter++) {
            final String source = "chronicle.test.race" + iter;
            final CapturingMetricsOut capture = new CapturingMetricsOut();
            final CyclicBarrier barrier = new CyclicBarrier(2);
            final AtomicReference<MetricsOut> facade = new AtomicReference<>();
            final Thread resolver = new Thread(() -> {
                await(barrier);
                facade.set(Metrics.forSource(source));
            }, "resolver");
            resolver.start();

            await(barrier);
            final Metrics.Installation installation =
                    Metrics.install(s -> source.equals(s) ? capture : null);
            resolver.join();
            try {
                facade.get().counterMetric(new CounterMetric()
                        .source(source).name("chronicle_test_races_total").count(1).delta(1));
                assertEquals("iter=" + iter
                                + ": facade resolved concurrently with install() is bound to a stale handler",
                        1, capture.metrics.size());
            } finally {
                installation.close();
                Metrics.resetForTesting();
            }
        }
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
