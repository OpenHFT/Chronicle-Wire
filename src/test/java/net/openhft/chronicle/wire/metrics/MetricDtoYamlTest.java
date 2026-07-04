/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

import net.openhft.chronicle.core.util.Histogram;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * YAML round-trips for each metric kind, asserting the text carries only that kind's fields
 * (the method name is the discriminator - no type field, no other kind's fields).
 */
public class MetricDtoYamlTest extends WireTestCommon {

    static final long EVENT_TIME = 1_000_000_000L;

    static void assertOnlyOwnFields(String text, String... ownFields) {
        for (String field : ownFields)
            assertTrue("expected " + field + " in " + text, text.contains(field + ":"));
        String[] allKindFields = {"count", "delta", "value", "perSecond", "histogram"};
        for (String field : allKindFields) {
            boolean own = false;
            for (String ownField : ownFields)
                if (ownField.equals(field))
                    own = true;
            if (!own)
                assertFalse("unexpected " + field + " in " + text, text.contains(field + ":"));
        }
    }

    @Test
    public void counterMetricRoundTrip() {
        CounterMetric metric = new CounterMetric()
                .source("chronicle.fix.session")
                .name("chronicle_fix_session_resend_requests_total")
                .label("session", "LSE-PROD")
                .label("side", "acceptor")
                .eventTime(EVENT_TIME)
                .intervalNs(1_000_000_000L)
                .count(4711)
                .delta(2);

        String text = metric.toString();
        assertOnlyOwnFields(text, "count", "delta");
        // labels are a plain scalar on the wire, not a map
        assertTrue(text, text.contains("labels: session=LSE-PROD;side=acceptor"));

        CounterMetric copy = Marshallable.fromString(CounterMetric.class, text);
        assertEquals(metric, copy);
        assertEquals(4711, copy.count());
        assertEquals(2, copy.delta());
        assertEquals("chronicle.fix.session", copy.source());
        assertEquals("session=LSE-PROD;side=acceptor", copy.labels());
        assertEquals("LSE-PROD", copy.decodeLabels().get("session"));
        assertEquals(EVENT_TIME, copy.eventTime());
        assertEquals(1_000_000_000L, copy.intervalNs());
    }

    @Test
    public void labelAppendOrderIsPreserved() {
        CounterMetric metric = new CounterMetric()
                .label("b", "2")
                .label("a", "1")
                .label("c", "3");
        assertEquals("b=2;a=1;c=3", metric.labels());

        java.util.Iterator<java.util.Map.Entry<String, String>> entries =
                metric.decodeLabels().entrySet().iterator();
        java.util.Map.Entry<String, String> first = entries.next();
        assertEquals("b", first.getKey());
        assertEquals("2", first.getValue());
        assertEquals("a", entries.next().getKey());
        assertEquals("c", entries.next().getKey());
        assertFalse(entries.hasNext());
    }

    @Test
    public void labelKeyOrValueWithReservedCharactersThrows() {
        CounterMetric metric = new CounterMetric();
        assertLabelThrows(metric, "key=bad", "value", "key");
        assertLabelThrows(metric, "key;bad", "value", "key");
        assertLabelThrows(metric, "key", "value=bad", "value=bad");
        assertLabelThrows(metric, "key", "value;bad", "value;bad");
        // and the metric is left untouched
        assertEquals(null, metric.labels());
    }

    @Test
    public void labelKeyMustBePrometheusValidAndPresent() {
        CounterMetric metric = new CounterMetric();
        assertLabelThrows(metric, null, "value", "null");
        assertLabelThrows(metric, "", "value", "empty");
        assertLabelThrows(metric, "1digit_first", "value", "1digit_first");
        assertLabelThrows(metric, "has-dash", "value", "has-dash");
        assertLabelThrows(metric, "has space", "value", "has space");
        assertEquals(null, metric.labels());
        // a blank VALUE is allowed; a null value is not
        metric.label("blank_ok", "");
        assertEquals("blank_ok=", metric.labels());
        assertLabelThrows(metric, "key2", null, "key2");
    }

    @Test
    public void duplicateLabelKeyThrows() {
        CounterMetric metric = new CounterMetric()
                .label("session", "LSE-PROD")
                .label("side", "acceptor");
        assertLabelThrows(metric, "session", "OTHER", "session");
        // no silent overwrite happened
        assertEquals("session=LSE-PROD;side=acceptor", metric.labels());
        // a key that is a prefix/suffix of an existing key is NOT a duplicate
        metric.label("sess", "x").label("ide", "y");
        assertEquals("session=LSE-PROD;side=acceptor;sess=x;ide=y", metric.labels());
    }

    static void assertLabelThrows(CounterMetric metric, String key, String value, String inMessage) {
        try {
            metric.label(key, value);
            org.junit.Assert.fail("expected IllegalArgumentException for " + key + "=" + value);
        } catch (IllegalArgumentException e) {
            assertTrue("expected '" + inMessage + "' in: " + e.getMessage(),
                    e.getMessage().contains(inMessage));
        }
    }

    @Test
    public void labelsStringIsValidatedAsAWhole() {
        // valid forms pass and are stored verbatim
        assertEquals("a=1;b=2", new CounterMetric().labels("a=1;b=2").labels());
        assertEquals("a=", new CounterMetric().labels("a=").labels());
        assertEquals(null, new CounterMetric().labels(null).labels());

        assertLabelsThrows("", "labels");
        assertLabelsThrows("nopair", "key=value");
        assertLabelsThrows("a=1;;b=2", "key=value");
        assertLabelsThrows("a=1;b=2;", "key=value");
        assertLabelsThrows("=1", "key=value");
        assertLabelsThrows("1a=1", "1a");
        assertLabelsThrows("a-b=1", "a-b");
        assertLabelsThrows("a=1;a=2", "duplicate label key 'a'");
    }

    static void assertLabelsThrows(String labels, String inMessage) {
        try {
            new CounterMetric().labels(labels);
            org.junit.Assert.fail("expected IllegalArgumentException for labels: " + labels);
        } catch (IllegalArgumentException e) {
            assertTrue("expected '" + inMessage + "' in: " + e.getMessage(),
                    e.getMessage().contains(inMessage));
        }
    }

    @Test
    public void metricNameIsValidated() {
        assertEquals("valid_name:total", new CounterMetric().name("valid_name:total").name());
        assertNameThrows(null, "null");
        assertNameThrows("", "empty");
        assertNameThrows("1_leading_digit", "1_leading_digit");
        assertNameThrows("has-dash", "has-dash");
        assertNameThrows("has space", "has space");
        assertNameThrows("has.dot", "has.dot");
    }

    static void assertNameThrows(String name, String inMessage) {
        try {
            new CounterMetric().name(name);
            org.junit.Assert.fail("expected IllegalArgumentException for name: " + name);
        } catch (IllegalArgumentException e) {
            assertTrue("expected '" + inMessage + "' in: " + e.getMessage(),
                    e.getMessage().contains(inMessage));
        }
    }

    @Test
    public void decodeLabelsRoundTrips() {
        CounterMetric metric = new CounterMetric()
                .label("session", "LSE-PROD")
                .label("side", "acceptor");
        java.util.Map<String, String> decoded = metric.decodeLabels();
        assertEquals(2, decoded.size());
        assertEquals("LSE-PROD", decoded.get("session"));
        assertEquals("acceptor", decoded.get("side"));

        // re-encode via label() into a fresh metric and compare the raw string
        CounterMetric reEncoded = new CounterMetric();
        for (java.util.Map.Entry<String, String> entry : decoded.entrySet())
            reEncoded.label(entry.getKey(), entry.getValue());
        assertEquals(metric.labels(), reEncoded.labels());

        // the direct setter takes the concatenated form, validated as a whole
        CounterMetric direct = new CounterMetric().labels("session=LSE-PROD;side=acceptor");
        assertEquals(decoded, direct.decodeLabels());
    }

    @Test
    public void nullLabelsAreSafe() {
        CounterMetric metric = new CounterMetric();
        assertEquals(null, metric.labels());
        java.util.Map<String, String> decoded = metric.decodeLabels();
        assertTrue(decoded.isEmpty());
        // and a first label starts the string without a separator
        metric.label("queue", "orders-out");
        assertEquals("queue=orders-out", metric.labels());
    }

    @Test
    public void gaugeMetricRoundTrip() {
        GaugeMetric metric = new GaugeMetric()
                .source("chronicle.queue.tailer")
                .name("chronicle_queue_tailer_lag_bytes")
                .unit("bytes")
                .eventTime(EVENT_TIME)
                .intervalNs(1_000_000_000L)
                .value(128.5);

        String text = metric.toString();
        assertOnlyOwnFields(text, "value");

        GaugeMetric copy = Marshallable.fromString(GaugeMetric.class, text);
        assertEquals(metric, copy);
        assertEquals(128.5, copy.value(), 0.0);
        assertEquals("bytes", copy.unit());
    }

    @Test
    public void histogramMetricRoundTrip() {
        Histogram histogram = new Histogram();
        long sum = 0;
        for (int i = 1; i <= 1000; i++) {
            histogram.sampleNanos(i * 100L);
            sum += i * 100L;
        }

        HistogramMetric metric = new HistogramMetric()
                .histogram(new MetricHistogram().sampleFrom(histogram).sum(sum))
                .source("chronicle.queue.appender")
                .name("chronicle_queue_write_latency_ns")
                .eventTime(EVENT_TIME)
                .intervalNs(1_000_000_000L);

        String text = metric.toString();
        // "count" appears too, inside the embedded MetricHistogram
        assertOnlyOwnFields(text, "histogram", "count");
        assertTrue(text.contains("percentiles:"));
        assertTrue(text.contains("worst:"));
        assertTrue(text.contains("min:"));
        assertTrue(text.contains("max:"));
        assertTrue(text.contains("sum:"));

        HistogramMetric copy = Marshallable.fromString(HistogramMetric.class, text);
        assertEquals(metric, copy);
        MetricHistogram mh = copy.histogram();
        assertEquals(1000, mh.count());
        // sum is exact (maintained by the instrument); min/max are bucket-quantised
        assertEquals(50_050_000L, mh.sum());
        assertEquals(100.0, mh.min(), 100.0 / 64);
        assertEquals(100_000.0, mh.max(), 100_000.0 / 64);
        assertEquals(mh.max(), mh.worst(), 0.0);
        assertTrue(mh.min() <= mh.percentile(0));
        assertTrue(mh.worst() >= mh.percentile(0));
    }

    @Test
    public void rateMetricRoundTrip() {
        RateMetric metric = new RateMetric()
                .source("chronicle.services.runner")
                .name("chronicle_services_events_rate")
                .eventTime(EVENT_TIME)
                .intervalNs(1_000_000_000L)
                .count(360)
                .perSecond(3.5);

        String text = metric.toString();
        assertOnlyOwnFields(text, "count", "perSecond");

        RateMetric copy = Marshallable.fromString(RateMetric.class, text);
        assertEquals(metric, copy);
        assertEquals(360, copy.count());
        assertEquals(3.5, copy.perSecond(), 0.0);
    }

    @Test
    public void pointEventRoundTrip() {
        PointEvent metric = new PointEvent()
                .source("chronicle.queue.replication")
                .name("chronicle_queue_replication_reconnects_total")
                .eventTime(EVENT_TIME)
                .value(1);

        String text = metric.toString();
        assertOnlyOwnFields(text, "value");

        PointEvent copy = Marshallable.fromString(PointEvent.class, text);
        assertEquals(metric, copy);
        assertEquals(1.0, copy.value(), 0.0);
        assertEquals(0L, copy.intervalNs());
    }

    @Test
    public void adapterFunnelsAllFiveKinds() {
        final java.util.List<Metric<?>> seen = new java.util.ArrayList<>();
        MetricsOut.Adapter adapter = seen::add;

        adapter.counterMetric(new CounterMetric().name("c"));
        adapter.gaugeMetric(new GaugeMetric().name("g"));
        adapter.histogramMetric(new HistogramMetric().name("h"));
        adapter.rateMetric(new RateMetric().name("r"));
        adapter.pointEvent(new PointEvent().name("p"));

        assertEquals(5, seen.size());
        assertEquals("c", seen.get(0).name());
        assertTrue(seen.get(0) instanceof CounterMetric);
        assertTrue(seen.get(1) instanceof GaugeMetric);
        assertTrue(seen.get(2) instanceof HistogramMetric);
        assertTrue(seen.get(3) instanceof RateMetric);
        assertTrue(seen.get(4) instanceof PointEvent);
    }
}
