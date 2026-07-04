/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A rate instrument: {@link #inc()} / {@link #inc(long)} count occurrences with thread-safe
 * atomic updates; the flush emits the running total plus the observed per-second rate over
 * the window through the instrument's reused {@link RateMetric}.
 */
public class RateInstrument extends Instrument {

    private static final double NANOS_PER_SECOND = 1e9;

    private final RateMetric metric;

    // Atomic so shared owners can record while a monitor thread flushes the window.
    private final AtomicLong count = new AtomicLong();
    private final AtomicLong lastFlushedCount = new AtomicLong();

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
     * @throws IllegalArgumentException if {@code key} is invalid or duplicate, or {@code value} is {@code null}
     */
    public RateInstrument label(String key, String value) {
        addLabel(metric, key, value);
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
     * Counts one occurrence. Hot path: no allocation; one atomic add.
     */
    public void inc() {
        inc(1);
    }

    /**
     * Counts {@code n} occurrences. Hot path: no allocation; one atomic add.
     *
     * @param n the number of occurrences
     */
    public void inc(long n) {
        if (n < 0)
            throw new IllegalArgumentException(
                    "negative delta " + n + " on rate '" + metric.name() + "'");
        count.addAndGet(n);
    }

    /**
     * Returns the running total of occurrences.
     *
     * @return the count
     */
    public long count() {
        return count.get();
    }

    @Override
    void flushTo(MetricsOut out, long eventTime, long intervalNs) {
        long c = count.get();
        long previous = lastFlushedCount.getAndSet(c);
        double perSecond = intervalNs > 0 ? (c - previous) * NANOS_PER_SECOND / intervalNs : 0.0;
        out.rateMetric(metric.count(c).perSecond(perSecond)
                .eventTime(eventTime).intervalNs(intervalNs));
    }

    @Override
    void rollWindow() {
        lastFlushedCount.set(count.get());
    }
}
