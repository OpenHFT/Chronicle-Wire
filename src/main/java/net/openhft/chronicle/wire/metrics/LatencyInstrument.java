/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

import net.openhft.chronicle.core.util.Histogram;

/**
 * A latency instrument: {@link #record(long)} samples into a {@link Histogram}
 * (Chronicle-Core's zero-allocation histogram, the same one JLBH uses) intended to be
 * thread-confined to the recording thread; the flush copies the histogram's state into the
 * instrument's reused {@link MetricHistogram} in place, emits the reused
 * {@link HistogramMetric}, and resets the histogram for the next window.
 */
public class LatencyInstrument extends Instrument {

    private final Histogram histogram;
    private final HistogramMetric metric;

    // Window sum of recorded durations. Core's Histogram does not track a sum (and its mean
    // is not exposed), so it is maintained here: one long add per record(), allocation-free.
    // Written by the recording thread with plain stores; read and reset at flush cadence.
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
     * @throws IllegalArgumentException if {@code key} or {@code value} contains {@code '='} or {@code ';'}
     */
    public LatencyInstrument label(String key, String value) {
        metric.label(key, value);
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
    public void record(long durationNs) {
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
    public Histogram histogram() {
        return histogram;
    }

    @Override
    void flushTo(MetricsOut out, long eventTime, long intervalNs) {
        metric.histogram().sampleFrom(histogram).sum(sum);
        out.histogramMetric(metric.eventTime(eventTime).intervalNs(intervalNs));
        histogram.reset();
        sum = 0;
    }
}
