/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

/**
 * A rate instrument: {@link #inc()} / {@link #inc(long)} count occurrences with plain-field
 * updates intended to be thread-confined to the recording thread; the flush emits the running
 * total plus the observed per-second rate over the window through the instrument's reused
 * {@link RateMetric}.
 */
public class RateInstrument extends Instrument {

    private static final double NANOS_PER_SECOND = 1e9;

    private final RateMetric metric;

    // Written by the recording thread with plain stores; read at flush cadence.
    private long count;

    // Only touched by the flusher.
    private long lastFlushedCount;

    RateInstrument(String source, String name) {
        metric = new RateMetric().source(source).name(name);
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
    public RateInstrument label(String key, String value) {
        metric.label(key, value);
        return this;
    }

    /**
     * Sets the registration-time unit of this instrument's metric.
     *
     * @param unit the unit
     * @return this instance for chaining
     */
    public RateInstrument unit(String unit) {
        metric.unit(unit);
        return this;
    }

    /**
     * Counts one occurrence. Hot path: no allocation, no volatile store.
     */
    public void inc() {
        count++;
    }

    /**
     * Counts {@code n} occurrences. Hot path: no allocation, no volatile store.
     *
     * @param n the number of occurrences
     */
    public void inc(long n) {
        count += n;
    }

    /**
     * Returns the running total of occurrences.
     *
     * @return the count
     */
    public long count() {
        return count;
    }

    @Override
    void flushTo(MetricsOut out, long eventTime, long intervalNs) {
        long c = count;
        double perSecond = intervalNs > 0 ? (c - lastFlushedCount) * NANOS_PER_SECOND / intervalNs : 0.0;
        out.rateMetric(metric.count(c).perSecond(perSecond)
                .eventTime(eventTime).intervalNs(intervalNs));
        lastFlushedCount = c;
    }
}
