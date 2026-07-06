/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

import net.openhft.chronicle.core.util.Histogram;
import net.openhft.chronicle.core.util.IgnoresEverything;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Acquisition, lifecycle and registry semantics: {@code forSource} always returns the stable
 * facade (ignored until a binding is installed, observing later installs);
 * {@code forSourceStatic} keeps the resolve-once raw-ignored fast path; installs are scoped
 * and resettable; registries dedup by identity, deregister on close, and per-owner registries
 * are independent.
 */
public class MetricsTest extends WireTestCommon {

    @After
    public void resetMetrics() {
        Metrics.resetForTesting();
    }

    @Test
    public void installAfterStaticResolveWarnsAboutPermanentlyNoOpSources() {
        // the classic "why are there no metrics in prod" trap: a component cached a
        // resolve-once handle before install(); the handle is permanently no-op and the
        // operator gets no signal - install() must name the affected sources once
        expectException("permanently no-op");
        assertTrue(Metrics.forSourceStatic("chronicle.test.early") instanceof IgnoresEverything);

        CapturingMetricsOut capture = new CapturingMetricsOut();
        try (Metrics.Installation installation = Metrics.install(source -> capture)) {
            assertNotNull(installation);
        }
    }

    @Test
    public void defaultIsIgnoredAndSafe() {
        // the resolve-once fast path is the raw ignored proxy, instanceof-skippable
        assertTrue(Metrics.forSourceStatic("chronicle.test.unbound") instanceof IgnoresEverything);

        // forSource is the stable facade: not IgnoresEverything itself, but resolves to it
        MetricsOut out = Metrics.forSource("chronicle.test.unbound");
        assertFalse(out instanceof IgnoresEverything);
        assertTrue(out instanceof ThreadLocalisedMetricsOut);
        assertTrue(ThreadLocalisedMetricsOut.unwrap(out) instanceof IgnoresEverything);

        // never null, never throws
        out.counterMetric(new CounterMetric().name("x").count(1).delta(1));
        out.gaugeMetric(new GaugeMetric().name("x").value(1));
        out.histogramMetric(new HistogramMetric().name("x"));
        out.rateMetric(new RateMetric().name("x"));
        out.pointEvent(new PointEvent().name("x").value(1));

        // handles are stable per source
        assertSame(out, Metrics.forSource("chronicle.test.unbound"));
        assertSame(Metrics.forSource(MetricsTest.class), Metrics.forSource(MetricsTest.class.getName()));
    }

    @Test
    public void facadeObservesLateInstall() {
        // cache the handle BEFORE any binding exists, as a static final field would
        MetricsOut cached = Metrics.forSource("chronicle.test.late");
        assertTrue(ThreadLocalisedMetricsOut.unwrap(cached) instanceof IgnoresEverything);
        cached.counterMetric(new CounterMetric().name("dropped").count(1).delta(1));

        CapturingMetricsOut capture = new CapturingMetricsOut();
        Metrics.install(source -> capture);

        // the cached handle observes the install: events flow now
        cached.counterMetric(new CounterMetric().name("flows").count(2).delta(1));
        assertEquals(1, capture.size());
        assertEquals("counterMetric", capture.methods.get(0));
        assertEquals("flows", capture.metrics.get(0).name());
        assertEquals(2, capture.<CounterMetric>metric(0).count());
    }

    @Test
    public void registryFlushSkipsIgnoredFacadeWithoutTouchingInstruments() {
        MetricsRegistry registry = Metrics.newRegistry("chronicle.test.flush.ignored");
        CounterInstrument counter = registry.counter("chronicle_test_flush_ignored_total");
        LatencyInstrument latency = registry.latency("chronicle_test_flush_ignored_latency_ns");
        counter.inc();
        latency.record(1000);

        MetricsOut ignoredFacade = Metrics.forSource("chronicle.test.flush.ignored");
        assertTrue(ThreadLocalisedMetricsOut.unwrap(ignoredFacade) instanceof IgnoresEverything);
        registry.flush(ignoredFacade, 1, 1);

        latency.record(2000);
        CapturingMetricsOut capture = new CapturingMetricsOut();
        registry.flush(capture, 2, 1);
        assertEquals(2, capture.size());
        assertEquals(0, capture.<CounterMetric>metric(0).delta());
        HistogramMetric histogram = capture.metric(1);
        assertEquals(1, histogram.histogram().count());
        assertEquals(2000, histogram.histogram().sum());
    }

    @Test
    public void forSourceStaticIsResolveOnce() {
        // resolving statically before install is exactly the trap install() now warns about
        expectException("permanently no-op");
        MetricsOut preInstall = Metrics.forSourceStatic("chronicle.test.fixed");
        assertTrue(preInstall instanceof IgnoresEverything);

        CapturingMetricsOut capture = new CapturingMetricsOut();
        Metrics.install(source -> capture);

        // the pre-install handle does NOT observe the install - documented trade-off
        preInstall.counterMetric(new CounterMetric().name("still_dropped").count(1).delta(1));
        assertEquals(0, capture.size());

        // resolved now, it is the raw bound handler
        assertSame(capture, Metrics.forSourceStatic("chronicle.test.fixed"));
    }

    @Test
    public void installedBindingResolvesSourcesAndUninstallRestoresIgnored() {
        final CapturingMetricsOut capture = new CapturingMetricsOut();
        final SourceSelection selection = new SourceSelection(
                java.util.Collections.singletonMap("chronicle.test.off", Boolean.FALSE));
        Metrics.install(source -> selection.enabled(source) ? capture : null);

        MetricsOut on = Metrics.forSource("chronicle.test.on");
        MetricsOut off = Metrics.forSource("chronicle.test.off.subsystem");

        on.counterMetric(new CounterMetric().name("hit").count(1).delta(1));
        off.counterMetric(new CounterMetric().name("miss").count(1).delta(1));
        assertEquals(1, capture.size());
        assertEquals("hit", capture.metrics.get(0).name());

        // handles are stable facades; uninstalling swaps their default back
        Metrics.install(null);
        on.counterMetric(new CounterMetric().name("late").count(2).delta(1));
        assertEquals(1, capture.size());
        assertTrue(ThreadLocalisedMetricsOut.unwrap(Metrics.forSource("chronicle.test.on"))
                instanceof IgnoresEverything);
        assertTrue(ThreadLocalisedMetricsOut.unwrap(on) instanceof IgnoresEverything);
    }

    @Test
    public void installHandleCloseRestoresPreviousBinding() {
        final CapturingMetricsOut outer = new CapturingMetricsOut();
        final CapturingMetricsOut inner = new CapturingMetricsOut();
        MetricsOut handle = Metrics.forSource("chronicle.test.scoped");

        Metrics.install(source -> outer);
        handle.counterMetric(new CounterMetric().name("one").count(1).delta(1));
        assertEquals(1, outer.size());

        Metrics.Installation scoped = Metrics.install(source -> inner);
        handle.counterMetric(new CounterMetric().name("two").count(2).delta(1));
        assertEquals(1, inner.size());
        assertEquals(1, outer.size());

        scoped.close();
        handle.counterMetric(new CounterMetric().name("three").count(3).delta(1));
        // the previous (outer) binding is restored ...
        assertEquals(2, outer.size());
        assertEquals("three", outer.metrics.get(1).name());
        assertEquals(1, inner.size());

        // ... and close() is idempotent: a second close does not re-install or disturb it
        scoped.close();
        handle.counterMetric(new CounterMetric().name("four").count(4).delta(1));
        assertEquals(3, outer.size());
        assertEquals(1, inner.size());
    }

    @Test
    public void resetForTestingIsolatesSequentialTestCases() {
        // fake test case 1: binding installed, instruments registered and flushed
        CapturingMetricsOut capture1 = new CapturingMetricsOut();
        Metrics.install(source -> capture1);
        MetricsRegistry registry1 = Metrics.registry("chronicle.test.isolation");
        registry1.counter("chronicle_test_case1_total").inc();
        registry1.flush(capture1, 1, 1);
        assertEquals(1, capture1.size());

        Metrics.resetForTesting();

        // fake test case 2: pristine state - no binding, an empty fresh registry
        assertTrue(Metrics.forSourceStatic("chronicle.test.isolation") instanceof IgnoresEverything);
        assertTrue(ThreadLocalisedMetricsOut.unwrap(Metrics.forSource("chronicle.test.isolation"))
                instanceof IgnoresEverything);
        MetricsRegistry registry2 = Metrics.registry("chronicle.test.isolation");
        assertNotSame(registry1, registry2);
        CapturingMetricsOut capture2 = new CapturingMetricsOut();
        registry2.flush(capture2, 2, 1);
        assertEquals("case 1's instruments must not leak into case 2", 0, capture2.size());
        // and the closed old registry no longer emits anywhere
        registry1.flush(capture2, 3, 1);
        assertEquals(0, capture2.size());
    }

    @Test
    public void newRegistriesAreIndependentPerOwner() {
        // the two-event-loop scenario: each loop owns its registry and flush cadence
        MetricsRegistry loopA = Metrics.newRegistry("chronicle.test.loops");
        MetricsRegistry loopB = Metrics.newRegistry("chronicle.test.loops");
        assertNotSame(loopA, loopB);
        // and both are independent of the process-global registry for the same source
        assertNotSame(loopA, Metrics.registry("chronicle.test.loops"));

        CounterInstrument counterA = loopA.counter("chronicle_test_loop_total");
        CounterInstrument counterB = loopB.counter("chronicle_test_loop_total");
        assertNotSame(counterA, counterB);
        counterA.inc(3);
        counterB.inc(5);

        CapturingMetricsOut captureA = new CapturingMetricsOut();
        loopA.flush(captureA, 1, 1);
        // flushing A touches only A's instruments ...
        assertEquals(1, captureA.size());
        assertEquals(3, captureA.<CounterMetric>metric(0).count());

        // ... and leaves B untouched: B's later flush still reports its full window
        CapturingMetricsOut captureB = new CapturingMetricsOut();
        loopB.flush(captureB, 2, 2);
        assertEquals(1, captureB.size());
        assertEquals(5, captureB.<CounterMetric>metric(0).count());
        assertEquals(5, captureB.<CounterMetric>metric(0).delta());
    }

    @Test
    public void registeringTheSameIdentityReturnsTheSameInstrument() {
        MetricsRegistry registry = Metrics.newRegistry("chronicle.test.dedup");

        CounterInstrument counter = registry.counter("chronicle_test_dedup_total", "queue=orders");
        assertSame(counter, registry.counter("chronicle_test_dedup_total", "queue=orders"));
        // different labels, or no labels, is a different series
        assertNotSame(counter, registry.counter("chronicle_test_dedup_total", "queue=trades"));
        assertNotSame(counter, registry.counter("chronicle_test_dedup_total"));
        assertSame(registry.counter("chronicle_test_dedup_total"),
                registry.counter("chronicle_test_dedup_total", null));

        assertSame(registry.gauge("chronicle_test_dedup_lag"), registry.gauge("chronicle_test_dedup_lag"));
        assertSame(registry.latency("chronicle_test_dedup_ns"), registry.latency("chronicle_test_dedup_ns"));
        assertSame(registry.rate("chronicle_test_dedup_rate"), registry.rate("chronicle_test_dedup_rate"));

        CounterInstrument fluent = registry.counter("chronicle_test_fluent_total")
                .label("queue", "orders-out");
        assertSame("fluent labels re-key the registered instrument",
                fluent, registry.counter("chronicle_test_fluent_total", "queue=orders-out"));
        assertNotSame("the original unlabelled identity is now free for a new instrument",
                fluent, registry.counter("chronicle_test_fluent_total"));

        CounterInstrument duplicate = registry.counter("chronicle_test_duplicate_total");
        registry.counter("chronicle_test_duplicate_total", "queue=orders-out");
        try {
            duplicate.label("queue", "orders-out");
            fail("expected duplicate identity re-key to fail");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("duplicate metric identity"));
        }

        // dedup means no duplicate series for repeated identities; every distinct identity emits once.
        counter.inc();
        CapturingMetricsOut capture = new CapturingMetricsOut();
        registry.flush(capture, 1, 1);
        assertEquals(10, capture.size());
        assertEquals("queue=orders", capture.metrics.get(0).labels());
    }

    @Test
    public void closingAnInstrumentStopsItsEmission() {
        MetricsRegistry registry = Metrics.newRegistry("chronicle.test.deregister");
        CounterInstrument keep = registry.counter("chronicle_test_keep_total");
        CounterInstrument drop = registry.counter("chronicle_test_drop_total");
        keep.inc();
        drop.inc();

        drop.close();
        drop.close(); // idempotent

        CapturingMetricsOut capture = new CapturingMetricsOut();
        registry.flush(capture, 1, 1);
        assertEquals(1, capture.size());
        assertEquals("chronicle_test_keep_total", capture.metrics.get(0).name());

        // recording into a closed instrument stays safe, just unemitted
        drop.inc();

        // a closed identity can be re-registered as a fresh series
        CounterInstrument fresh = registry.counter("chronicle_test_drop_total");
        assertNotSame(drop, fresh);
    }

    @Test
    public void closingARegistryStopsAllEmissionAndRejectsRegistration() {
        MetricsRegistry registry = Metrics.newRegistry("chronicle.test.close");
        CounterInstrument counter = registry.counter("chronicle_test_close_total");
        counter.inc();

        assertFalse(registry.isClosed());
        registry.close();
        registry.close(); // idempotent
        assertTrue(registry.isClosed());

        CapturingMetricsOut capture = new CapturingMetricsOut();
        registry.flush(capture, 1, 1);
        assertEquals(0, capture.size());

        try {
            registry.counter("chronicle_test_after_close_total");
            fail("expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("chronicle.test.close"));
        }
    }

    @Test
    public void throwingBindingIsContainedAndSourceIgnored() {
        Metrics.install(source -> {
            throw new IllegalStateException("broken binding");
        });

        // acquisition and recording must not throw into callers
        MetricsOut out = Metrics.forSource("chronicle.test.broken");
        out.counterMetric(new CounterMetric().name("x").count(1).delta(1));
        assertTrue(ThreadLocalisedMetricsOut.unwrap(out) instanceof IgnoresEverything);
        assertTrue(Metrics.forSourceStatic("chronicle.test.broken") instanceof IgnoresEverything);

        // the containment is logged once as a warning (the named source is whichever
        // facade was re-resolved first, as install() re-resolves every known source)
        expectException("threw; treating the source as ignored");
    }

    @Test
    public void negativeCounterDeltaThrowsUnlessAllowed() {
        MetricsRegistry registry = Metrics.newRegistry("chronicle.test.negative");
        CounterInstrument monotonic = registry.counter("chronicle_test_monotonic_total");
        monotonic.inc(2);
        try {
            monotonic.inc(-1);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("-1"));
            assertTrue(e.getMessage(), e.getMessage().contains("chronicle_test_monotonic_total"));
            assertTrue(e.getMessage(), e.getMessage().contains("allowNegative"));
        }
        assertEquals(2, monotonic.count());

        CounterInstrument signed = registry.counter("chronicle_test_signed_total", "kind=signed")
                .allowNegative();
        signed.inc(2);
        signed.inc(-3);
        assertEquals(-1, signed.count());
    }

    @Test
    public void negativeRatesAndLatenciesThrow() {
        MetricsRegistry registry = Metrics.newRegistry("chronicle.test.negative.more");
        try {
            registry.rate("chronicle_test_rate").inc(-1);
            fail("expected negative rate increment to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("-1"));
            assertTrue(e.getMessage(), e.getMessage().contains("chronicle_test_rate"));
        }
        try {
            registry.latency("chronicle_test_latency_ns").record(-1);
            fail("expected negative latency to fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("-1"));
            assertTrue(e.getMessage(), e.getMessage().contains("chronicle_test_latency_ns"));
        }
    }

    @Test
    public void emptyHistogramSnapshotUsesNanWindowValues() {
        MetricHistogram histogram = new MetricHistogram().sampleFrom(new Histogram()).sum(0);
        assertEquals(0, histogram.count());
        assertEquals(0, histogram.sum());
        assertTrue(Double.isNaN(histogram.min()));
        assertTrue(Double.isNaN(histogram.max()));
        assertTrue(Double.isNaN(histogram.worst()));
        for (int i = 0; i < MetricHistogram.percentileFractions().length; i++)
            assertTrue(Double.isNaN(histogram.percentile(i)));
    }

    @Test
    public void invalidRegistrationNamesThrow() {
        MetricsRegistry registry = Metrics.newRegistry("chronicle.test.names");
        assertNameRejected(registry, "1starts_with_digit");
        assertNameRejected(registry, "has-dash");
        assertNameRejected(registry, "has space");
        assertNameRejected(registry, "");
        assertNameRejected(registry, null);
        // valid Prometheus names register fine, including leading '_' and ':'
        registry.counter("_underscore_first");
        registry.gauge(":colon:first");
    }

    static void assertNameRejected(MetricsRegistry registry, String name) {
        try {
            registry.counter(name);
            fail("expected IllegalArgumentException for name: " + name);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(String.valueOf(name)));
        }
    }

    @Test
    public void registryFlushesEachKind() {
        MetricsRegistry registry = Metrics.registry("chronicle.test.registry");
        assertSame(registry, Metrics.registry("chronicle.test.registry"));
        assertEquals("chronicle.test.registry", registry.source());

        CounterInstrument counter = registry.counter("chronicle_test_events_total")
                .label("queue", "orders-out");
        GaugeInstrument gauge = registry.gauge("chronicle_test_lag_bytes").unit("bytes");
        LatencyInstrument latency = registry.latency("chronicle_test_write_latency_ns");
        RateInstrument rate = registry.rate("chronicle_test_reads_rate");

        counter.inc();
        counter.inc(2);
        gauge.set(12.5);
        for (int i = 1; i <= 10; i++)
            latency.record(i * 1000L);
        rate.inc(5);

        CapturingMetricsOut capture = new CapturingMetricsOut();
        long second = 1_000_000_000L;
        registry.flush(capture, second, second);

        assertEquals(4, capture.size());
        assertEquals("counterMetric", capture.methods.get(0));
        CounterMetric counterMetric = capture.metric(0);
        assertEquals(3, counterMetric.count());
        assertEquals(3, counterMetric.delta());
        assertEquals("chronicle.test.registry", counterMetric.source());
        assertEquals("chronicle_test_events_total", counterMetric.name());
        assertEquals("queue=orders-out", counterMetric.labels());
        assertEquals("orders-out", counterMetric.decodeLabels().get("queue"));
        assertEquals(second, counterMetric.eventTime());
        assertEquals(second, counterMetric.intervalNs());

        assertEquals("gaugeMetric", capture.methods.get(1));
        GaugeMetric gaugeMetric = capture.metric(1);
        assertEquals(12.5, gaugeMetric.value(), 0.0);
        assertEquals("bytes", gaugeMetric.unit());

        assertEquals("histogramMetric", capture.methods.get(2));
        HistogramMetric histogramMetric = capture.metric(2);
        assertEquals(10, histogramMetric.histogram().count());
        // sum is exact (maintained by record()); min/max are bucket-quantised
        assertEquals(55_000L, histogramMetric.histogram().sum());
        assertEquals(1000.0, histogramMetric.histogram().min(), 1000.0 / 64);
        assertEquals(10_000.0, histogramMetric.histogram().max(), 10_000.0 / 64);

        assertEquals("rateMetric", capture.methods.get(3));
        RateMetric rateMetric = capture.metric(3);
        assertEquals(5, rateMetric.count());
        assertEquals(5.0, rateMetric.perSecond(), 0.001);

        // second window: deltas, the histogram and its sum reset, running totals continue
        counter.inc();
        latency.record(7000L);
        registry.flush(capture, 2 * second, second);
        assertEquals(8, capture.size());
        CounterMetric counterMetric2 = capture.metric(4);
        assertEquals(4, counterMetric2.count());
        assertEquals(1, counterMetric2.delta());
        HistogramMetric histogramMetric2 = capture.metric(6);
        assertEquals(1, histogramMetric2.histogram().count());
        assertEquals(7000L, histogramMetric2.histogram().sum());
        RateMetric rateMetric2 = capture.metric(7);
        assertEquals(5, rateMetric2.count());
        assertEquals(0.0, rateMetric2.perSecond(), 0.0);
    }

    @Test
    public void latencyInstrumentPublishesConfiguredWindowsIndependently() {
        MetricsRegistry registry = Metrics.newRegistry("chronicle.test.windows");
        LatencyInstrument latency = registry.latency("chronicle_test_latency_ns")
                .label("queue", "orders")
                .unit("ns")
                .windowIntervalsNs(8, 2, 4);

        CapturingMetricsOut capture = new CapturingMetricsOut();
        for (int i = 1; i <= 8; i++) {
            latency.record(i * 1000L);
            registry.flush(capture, i, 1);
        }

        assertEquals(7, capture.size());
        assertHistogramWindow(capture, 0, 2, 2, 3_000L, 2);
        assertHistogramWindow(capture, 1, 2, 2, 7_000L, 4);
        assertHistogramWindow(capture, 2, 4, 4, 10_000L, 4);
        assertHistogramWindow(capture, 3, 2, 2, 11_000L, 6);
        assertHistogramWindow(capture, 4, 2, 2, 15_000L, 8);
        assertHistogramWindow(capture, 5, 4, 4, 26_000L, 8);
        assertHistogramWindow(capture, 6, 8, 8, 36_000L, 8);
    }

    @Test
    public void latencyWindowIntervalsRejectInvalidValues() {
        LatencyInstrument latency = Metrics.newRegistry("chronicle.test.bad.windows")
                .latency("chronicle_test_latency_ns");

        assertWindowRejected(latency);
        assertWindowRejected(latency, 0);
        assertWindowRejected(latency, 1, 1);
        assertWindowRejected(latency, -1, 1);
    }

    private static void assertHistogramWindow(CapturingMetricsOut capture, int index,
                                              long intervalNs, long count, long sum, long eventTime) {
        assertEquals("histogramMetric", capture.methods.get(index));
        HistogramMetric metric = capture.metric(index);
        assertEquals(intervalNs, metric.intervalNs());
        assertEquals(eventTime, metric.eventTime());
        assertEquals(count, metric.histogram().count());
        assertEquals(sum, metric.histogram().sum());
        assertEquals("queue=orders", metric.labels());
        assertEquals("ns", metric.unit());
    }

    private static void assertWindowRejected(LatencyInstrument latency, long... intervalsNs) {
        try {
            latency.windowIntervalsNs(intervalsNs);
            fail("expected invalid latency window intervals to fail");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("interval"));
        }
    }

    @Test
    public void flushToIgnoredIsANoOp() {
        MetricsRegistry registry = Metrics.registry("chronicle.test.noop");
        CounterInstrument counter = registry.counter("chronicle_test_noop_total");
        counter.inc();
        // must not throw; disabled flushes roll the delta window forward without emission
        registry.flush(Metrics.ignored(), 1, 1);
        registry.flush(new ThreadLocalisedMetricsOut(null), 1, 1);
        assertEquals(1, counter.count());

        CapturingMetricsOut capture = new CapturingMetricsOut();
        registry.flush(capture, 2, 1);
        assertEquals(1, capture.size());
        assertEquals(1, capture.<CounterMetric>metric(0).count());
        assertEquals(0, capture.<CounterMetric>metric(0).delta());
    }

    @Test
    public void latencyInstrumentExposesItsHistogram() {
        MetricsRegistry registry = Metrics.registry("chronicle.test.latency");
        LatencyInstrument latency = registry.latency("chronicle_test_latency_ns");
        Histogram histogram = latency.histogram();
        latency.record(1000L);
        assertEquals(1, histogram.totalCount());
    }
}
