/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.util.Histogram;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * End-to-end loop: metric events written through a {@link MetricsOut} method writer into a
 * Wire, read back with a {@link MethodReader} into a capturing {@link MetricsOut}, asserting
 * that the method name is the discriminator and the field values survive.
 */
public class MetricsOutMethodWriterTest extends WireTestCommon {

    @Test
    public void methodWriterToMethodReaderText() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(512)).useTextDocuments();
        writeReadAndAssert(wire, true);
    }

    @Test
    public void methodWriterToMethodReaderBinary() {
        Wire wire = new BinaryWire(Bytes.allocateElasticOnHeap(512));
        writeReadAndAssert(wire, false);
    }

    private void writeReadAndAssert(Wire wire, boolean checkText) {
        MetricsOut writer = wire.methodWriter(MetricsOut.class);

        // build-once, mutate-and-re-emit, as production code would
        CounterMetric counter = new CounterMetric()
                .source("chronicle.fix.session")
                .name("chronicle_fix_session_resend_requests_total")
                .label("session", "LSE-PROD");
        writer.counterMetric(counter.count(4711).delta(2)
                .eventTime(1_000_000_000L).intervalNs(1_000_000_000L));

        GaugeMetric gauge = new GaugeMetric()
                .source("chronicle.queue.tailer")
                .name("chronicle_queue_tailer_lag_bytes");
        writer.gaugeMetric(gauge.value(42.5)
                .eventTime(1_000_000_000L).intervalNs(1_000_000_000L));

        Histogram histogram = new Histogram();
        for (int i = 1; i <= 100; i++)
            histogram.sampleNanos(i * 1000L);
        HistogramMetric histogramMetric = new HistogramMetric()
                .histogram(new MetricHistogram())
                .source("chronicle.queue.appender")
                .name("chronicle_queue_write_latency_ns");
        histogramMetric.histogram().sampleFrom(histogram);
        writer.histogramMetric(histogramMetric
                .eventTime(1_000_000_000L).intervalNs(1_000_000_000L));

        RateMetric rate = new RateMetric()
                .source("chronicle.services.runner")
                .name("chronicle_services_events_rate");
        writer.rateMetric(rate.count(360).perSecond(3.5)
                .eventTime(1_000_000_000L).intervalNs(1_000_000_000L));

        PointEvent point = new PointEvent()
                .source("chronicle.queue.replication")
                .name("chronicle_queue_replication_reconnects_total");
        writer.pointEvent(point.value(1).eventTime(2_000_000_000L));

        // re-emit the same counter instance with new values, as a flusher would
        writer.counterMetric(counter.count(4713).delta(2)
                .eventTime(2_000_000_000L).intervalNs(1_000_000_000L));

        if (checkText) {
            String text = wire.toString();
            // the method name is the discriminator - no type field, no alias
            assertTrue(text, text.contains("counterMetric:"));
            assertTrue(text, text.contains("gaugeMetric:"));
            assertTrue(text, text.contains("histogramMetric:"));
            assertTrue(text, text.contains("rateMetric:"));
            assertTrue(text, text.contains("pointEvent:"));
        }

        CapturingMetricsOut capture = new CapturingMetricsOut();
        MethodReader reader = wire.methodReader(capture);
        while (reader.readOne()) {
            // drain
        }

        assertEquals(6, capture.size());
        assertEquals("counterMetric", capture.methods.get(0));
        assertEquals("gaugeMetric", capture.methods.get(1));
        assertEquals("histogramMetric", capture.methods.get(2));
        assertEquals("rateMetric", capture.methods.get(3));
        assertEquals("pointEvent", capture.methods.get(4));
        assertEquals("counterMetric", capture.methods.get(5));

        CounterMetric counter0 = capture.metric(0);
        assertEquals(4711, counter0.count());
        assertEquals(2, counter0.delta());
        assertEquals("chronicle.fix.session", counter0.source());
        assertEquals("session=LSE-PROD", counter0.labels());
        assertEquals("LSE-PROD", counter0.decodeLabels().get("session"));
        assertEquals(1_000_000_000L, counter0.eventTime());

        GaugeMetric gauge1 = capture.metric(1);
        assertEquals(42.5, gauge1.value(), 0.0);
        assertEquals("chronicle_queue_tailer_lag_bytes", gauge1.name());

        HistogramMetric histogram2 = capture.metric(2);
        assertEquals(100, histogram2.histogram().count());
        assertEquals("chronicle.queue.appender", histogram2.source());

        RateMetric rate3 = capture.metric(3);
        assertEquals(360, rate3.count());
        assertEquals(3.5, rate3.perSecond(), 0.0);

        PointEvent point4 = capture.metric(4);
        assertEquals(1.0, point4.value(), 0.0);
        assertEquals(0L, point4.intervalNs());
        assertEquals(2_000_000_000L, point4.eventTime());

        CounterMetric counter5 = capture.metric(5);
        assertEquals(4713, counter5.count());
        assertEquals(2_000_000_000L, counter5.eventTime());
    }
}
