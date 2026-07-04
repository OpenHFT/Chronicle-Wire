/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

import net.openhft.chronicle.core.util.Histogram;

/**
 * A latency instrument: {@link #record(long)} samples into a {@link Histogram}
 * (Chronicle-Core's zero-allocation histogram, the same one JLBH uses). Recording and
 * window rollover are synchronized because the underlying histogram is mutable; the flush
 * copies the histogram's state into the instrument's reused {@link MetricHistogram} and
 * rolls the window <em>under</em> that monitor (bounded work, no I/O), then writes to the
 * sink <em>outside</em> it - a recording thread (e.g. an appender committing an excerpt)
 * is never blocked behind a slow sink. Flushing is single-flusher (the registry's flush
 * cadence); concurrent flushers would race on the reused DTO.
 */
public class LatencyInstrument extends Instrument {

    private final Histogram histogram;
    private final HistogramMetric metric;

    // Window sum of recorded durations. Core's Histogram does not track a sum (and its mean
    // is not exposed), so it is maintained here under the same monitor as the histogram.
    private long sum;

    LatencyInstrument(String source, String name) {
        this(source, name, new Histogram());
    }

    LatencyInstrument(String source, String name, Histogram histogram) {
        this.histogram = histogram;
        this.metric = new HistogramMetric().histogram(new MetricHistogram())
                .source(source).name(name);
    }

    @Override
    Metric<?> metric() {
        return metric;
    }

    /**
     * Adds a registration-time label to this instrument's metric.
     *
     * @param key   the label key
     * @param value the label value
     * @return this instance for chaining
     * @throws IllegalArgumentException if {@code key} is invalid or duplicate, or {@code value} is {@code null}
     */
    public LatencyInstrument label(String key, String value) {
        addLabel(metric, key, value);
        return this;
    }

    /**
     * Sets the registration-time unit of this instrument's metric.
     *
     * @param unit the unit
     * @return this instance for chaining
     */
    public LatencyInstrument unit(String unit) {
        metric.unit(unit);
        return this;
    }

    /**
     * Records one duration. Hot path: a few nanoseconds, no allocation.
     *
     * @param durationNs the duration in nanoseconds
     */
    public synchronized void record(long durationNs) {
        if (durationNs < 0)
            throw new IllegalArgumentException(
                    "negative latency " + durationNs + " for histogram '" + metric.name() + "'");
        histogram.sampleNanos(durationNs);
        sum += durationNs;
    }

    /**
     * Returns the underlying histogram; exposed for tests and owner-flush wiring. Note that
     * the window {@code sum} is maintained by {@link #record(long)}, not by the histogram -
     * samples fed to the histogram directly are not reflected in the emitted sum.
     *
     * @return the histogram samples accumulate into
     */
    public synchronized Histogram histogram() {
        return histogram;
    }

    @Override
    void flushTo(MetricsOut out, long eventTime, long intervalNs) {
        synchronized (this) {
            metric.histogram().sampleFrom(histogram).sum(sum);
            histogram.reset();
            sum = 0;
        }
        // sink write outside the monitor: record() must never wait on sink I/O
        out.histogramMetric(metric.eventTime(eventTime).intervalNs(intervalNs));
    }

    @Override
    synchronized void rollWindow() {
        histogram.reset();
        sum = 0;
    }
}
