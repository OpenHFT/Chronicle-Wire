/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

import org.junit.After;
import org.junit.Test;

import java.lang.management.ManagementFactory;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * The allocation gates for the metrics API's central promise (design 5.5): zero allocation
 * steady-state on the record paths ({@code inc()}, {@code record()}), the full registry
 * flush into a live binding (via the {@link ThreadLocalisedMetricsOut} facade, the handle
 * operators are told to cache), the {@code pointEvent} path, and the disabled flush.
 * Measured with {@code com.sun.management.ThreadMXBean#getThreadAllocatedBytes} after a
 * warmup that lets lazy initialisation and JIT settle.
 */
public class MetricsAllocationTest {

    private static final int WARMUP = 20_000;
    private static final int MEASURED = 100_000;
    // catches even a small per-operation allocation while tolerating measurement noise
    private static final long MAX_ALLOCATED_BYTES = 32 << 10;

    @After
    public void resetMetrics() {
        Metrics.resetForTesting();
    }

    /** A live (not IgnoresEverything) sink that only bumps primitive counters. */
    static final class CountingSink implements MetricsOut {
        long counters, gauges, histograms, rates, points;

        @Override
        public void counterMetric(CounterMetric metric) {
            counters++;
        }

        @Override
        public void gaugeMetric(GaugeMetric metric) {
            gauges++;
        }

        @Override
        public void histogramMetric(HistogramMetric metric) {
            histograms++;
        }

        @Override
        public void rateMetric(RateMetric metric) {
            rates++;
        }

        @Override
        public void pointEvent(PointEvent metric) {
            points++;
        }
    }

    @Test
    public void recordPathsAndFullFlushAllocateZeroSteadyState() {
        com.sun.management.ThreadMXBean allocations = allocationsBean();

        CountingSink sink = new CountingSink();
        Metrics.install(source -> sink);
        MetricsOut out = Metrics.forSource("chronicle.test.alloc");

        MetricsRegistry registry = Metrics.registry("chronicle.test.alloc");
        CounterInstrument counter = registry.counter("chronicle_test_ops_total");
        LatencyInstrument latency = registry.latency("chronicle_test_latency_ns");
        RateInstrument rate = registry.rate("chronicle_test_ops_per_second");
        GaugeInstrument gauge = registry.gauge("chronicle_test_depth");

        // 1. record paths, all four instrument kinds
        for (int i = 0; i < WARMUP; i++) {
            counter.inc();
            latency.record(i & 0xFFFF);
            rate.inc();
            gauge.set(i);
        }
        long allocated = measure(allocations, () -> {
            for (int i = 0; i < MEASURED; i++) {
                counter.inc();
                latency.record(i & 0xFFFF);
                rate.inc();
                gauge.set(i);
            }
        });
        assertTrue("record paths allocated " + allocated + " bytes over " + MEASURED
                + " iterations; expected <= " + MAX_ALLOCATED_BYTES, allocated <= MAX_ALLOCATED_BYTES);

        // 2. full flush of every instrument kind through the live facade
        for (int i = 0; i < 1_000; i++)
            registry.flush(out, i, 1);
        allocated = measure(allocations, () -> {
            for (int i = 0; i < 10_000; i++)
                registry.flush(out, i, 1);
        });
        assertTrue("full flush allocated " + allocated + " bytes over 10k flushes; expected <= "
                + MAX_ALLOCATED_BYTES, allocated <= MAX_ALLOCATED_BYTES);
        assertTrue("the sink really was live", sink.counters > 10_000);
        assertTrue(sink.gauges > 10_000);
        assertTrue(sink.rates > 10_000);
        assertTrue(sink.histograms > 10_000);
    }

    @Test
    public void pointEventEmissionAllocatesZeroSteadyState() {
        com.sun.management.ThreadMXBean allocations = allocationsBean();

        CountingSink sink = new CountingSink();
        Metrics.install(source -> sink);
        MetricsOut out = Metrics.forSource("chronicle.test.alloc");

        // the reused-DTO pattern every producer follows: identity set once, values per event
        PointEvent event = new PointEvent()
                .source("chronicle.test.alloc").name("chronicle_test_event").label("k", "v");
        for (int i = 0; i < WARMUP; i++)
            out.pointEvent(event.value(i).eventTime(i));
        long allocated = measure(allocations, () -> {
            for (int i = 0; i < MEASURED; i++)
                out.pointEvent(event.value(i).eventTime(i));
        });
        assertTrue("pointEvent path allocated " + allocated + " bytes over " + MEASURED
                + " events; expected <= " + MAX_ALLOCATED_BYTES, allocated <= MAX_ALLOCATED_BYTES);
        assertTrue(sink.points >= MEASURED);
    }

    @Test
    public void disabledFlushAllocatesZeroSteadyState() {
        com.sun.management.ThreadMXBean allocations = allocationsBean();

        // no binding installed: the facade resolves to the shared ignored proxy
        MetricsOut out = Metrics.forSource("chronicle.test.alloc.disabled");
        MetricsRegistry registry = Metrics.registry("chronicle.test.alloc.disabled");
        CounterInstrument counter = registry.counter("chronicle_test_ops_total");
        LatencyInstrument latency = registry.latency("chronicle_test_latency_ns");

        for (int i = 0; i < WARMUP; i++) {
            counter.inc();
            latency.record(i & 0xFFFF);
            registry.flush(out, i, 1);
        }
        long allocated = measure(allocations, () -> {
            for (int i = 0; i < MEASURED; i++) {
                counter.inc();
                latency.record(i & 0xFFFF);
                registry.flush(out, i, 1);
            }
        });
        assertTrue("disabled record+flush allocated " + allocated + " bytes over " + MEASURED
                + " iterations; expected <= " + MAX_ALLOCATED_BYTES, allocated <= MAX_ALLOCATED_BYTES);
    }

    @SuppressWarnings("deprecation") // Thread.getId(): Java 8 baseline
    private static long measure(com.sun.management.ThreadMXBean allocations, Runnable work) {
        final long threadId = Thread.currentThread().getId();
        allocations.getThreadAllocatedBytes(threadId); // prime the measurement path
        long before = allocations.getThreadAllocatedBytes(threadId);
        work.run();
        return allocations.getThreadAllocatedBytes(threadId) - before;
    }

    private static com.sun.management.ThreadMXBean allocationsBean() {
        java.lang.management.ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        assumeTrue("requires com.sun.management.ThreadMXBean",
                threadMXBean instanceof com.sun.management.ThreadMXBean);
        com.sun.management.ThreadMXBean allocations = (com.sun.management.ThreadMXBean) threadMXBean;
        assumeTrue(allocations.isThreadAllocatedMemorySupported());
        if (!allocations.isThreadAllocatedMemoryEnabled())
            allocations.setThreadAllocatedMemoryEnabled(true);
        return allocations;
    }
}
