/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

import net.openhft.chronicle.core.util.IgnoresEverything;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The {@code ThreadLocalisedExceptionHandler}-pattern contract: a default swap is visible to
 * every thread on its next call, per-thread overrides are isolated under concurrency, and
 * resetting an override restores the default.
 */
public class ThreadLocalisedMetricsOutTest extends WireTestCommon {

    static final long TIMEOUT_SECS = 10;

    @Test
    public void defaultSwapVisibleAcrossThreads() throws InterruptedException {
        final CapturingMetricsOut first = new CapturingMetricsOut();
        final CapturingMetricsOut second = new CapturingMetricsOut();
        final ThreadLocalisedMetricsOut out = new ThreadLocalisedMetricsOut(first);

        final CountDownLatch emittedToFirst = new CountDownLatch(1);
        final CountDownLatch swapped = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(1);

        Thread thread = new Thread(() -> {
            out.counterMetric(new CounterMetric().name("a"));
            emittedToFirst.countDown();
            try {
                if (!swapped.await(TIMEOUT_SECS, TimeUnit.SECONDS))
                    return;
            } catch (InterruptedException e) {
                return;
            }
            out.counterMetric(new CounterMetric().name("b"));
            done.countDown();
        });
        thread.start();

        assertTrue(emittedToFirst.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        out.defaultHandler(second);
        swapped.countDown();
        assertTrue(done.await(TIMEOUT_SECS, TimeUnit.SECONDS));
        thread.join();

        assertEquals(1, first.size());
        assertEquals("a", first.metrics.get(0).name());
        assertEquals(1, second.size());
        assertEquals("b", second.metrics.get(0).name());
        assertSame(second, out.defaultHandler());
    }

    @Test
    public void perThreadOverridesAreIsolated() throws InterruptedException {
        final CapturingMetricsOut fallback = new CapturingMetricsOut();
        final ThreadLocalisedMetricsOut out = new ThreadLocalisedMetricsOut(fallback);

        final CapturingMetricsOut capture1 = new CapturingMetricsOut();
        final CapturingMetricsOut capture2 = new CapturingMetricsOut();
        final CountDownLatch bothInstalled = new CountDownLatch(2);

        Runnable task1 = () -> emitWithOverride(out, capture1, "one", bothInstalled);
        Runnable task2 = () -> emitWithOverride(out, capture2, "two", bothInstalled);

        Thread thread1 = new Thread(task1);
        Thread thread2 = new Thread(task2);
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        assertEquals(2, capture1.size());
        assertEquals("one", capture1.metrics.get(0).name());
        assertEquals("one", capture1.metrics.get(1).name());
        assertEquals(2, capture2.size());
        assertEquals("two", capture2.metrics.get(0).name());
        assertEquals("two", capture2.metrics.get(1).name());
        // nothing leaked to the shared default
        assertEquals(0, fallback.size());
        // and the main thread has no override
        assertNull(out.threadLocal());
    }

    private void emitWithOverride(ThreadLocalisedMetricsOut out, CapturingMetricsOut capture,
                                  String name, CountDownLatch bothInstalled) {
        out.threadLocal(capture);
        try {
            bothInstalled.countDown();
            if (!bothInstalled.await(TIMEOUT_SECS, TimeUnit.SECONDS))
                return;
            out.counterMetric(new CounterMetric().name(name));
            out.pointEvent(new PointEvent().name(name));
        } catch (InterruptedException e) {
            // fall through to reset
        } finally {
            out.resetThreadLocal();
        }
    }

    @Test
    public void resetRestoresDefault() {
        final CapturingMetricsOut fallback = new CapturingMetricsOut();
        final CapturingMetricsOut override = new CapturingMetricsOut();
        final ThreadLocalisedMetricsOut out = new ThreadLocalisedMetricsOut(fallback);

        out.counterMetric(new CounterMetric().name("before"));
        assertEquals(1, fallback.size());

        out.threadLocal(override);
        assertSame(override, out.threadLocal());
        assertSame(override, out.unwrap());
        out.counterMetric(new CounterMetric().name("during"));
        assertEquals(1, fallback.size());
        assertEquals(1, override.size());
        assertEquals("during", override.metrics.get(0).name());

        out.resetThreadLocal();
        assertNull(out.threadLocal());
        assertSame(fallback, out.unwrap());
        out.counterMetric(new CounterMetric().name("after"));
        assertEquals(2, fallback.size());
        assertEquals("after", fallback.metrics.get(1).name());
        assertEquals(1, override.size());
    }

    @Test
    public void ignoredHandlersAreSkipped() {
        final ThreadLocalisedMetricsOut out = new ThreadLocalisedMetricsOut(null);
        assertTrue(out.defaultHandler() instanceof IgnoresEverything);
        assertTrue(out.unwrap() instanceof IgnoresEverything);
        // safe no-ops
        out.counterMetric(new CounterMetric().name("x"));
        out.gaugeMetric(new GaugeMetric().name("x"));
        out.histogramMetric(new HistogramMetric().name("x"));
        out.rateMetric(new RateMetric().name("x"));
        out.pointEvent(new PointEvent().name("x"));

        // unwrap(MetricsOut) sees through the facade
        assertSame(out.defaultHandler(), ThreadLocalisedMetricsOut.unwrap(out));
    }

    @Test
    public void delegateFailuresAreContainedAndWarnedOnce() {
        expectException("Metrics sink threw");
        final ThreadLocalisedMetricsOut out = new ThreadLocalisedMetricsOut(new ThrowingMetricsOut());
        out.counterMetric(new CounterMetric().name("x"));
        out.gaugeMetric(new GaugeMetric().name("x"));
        out.histogramMetric(new HistogramMetric().name("x"));
        out.rateMetric(new RateMetric().name("x"));
        out.pointEvent(new PointEvent().name("x"));
    }

    static final class ThrowingMetricsOut implements MetricsOut {
        @Override
        public void counterMetric(CounterMetric metric) {
            throw new IllegalStateException("broken counter sink");
        }

        @Override
        public void gaugeMetric(GaugeMetric metric) {
            throw new IllegalStateException("broken gauge sink");
        }

        @Override
        public void histogramMetric(HistogramMetric metric) {
            throw new IllegalStateException("broken histogram sink");
        }

        @Override
        public void rateMetric(RateMetric metric) {
            throw new IllegalStateException("broken rate sink");
        }

        @Override
        public void pointEvent(PointEvent metric) {
            throw new IllegalStateException("broken point sink");
        }
    }
}
