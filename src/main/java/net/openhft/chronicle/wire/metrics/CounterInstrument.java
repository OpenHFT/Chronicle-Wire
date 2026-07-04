/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A monotonic counter instrument: {@link #inc()} / {@link #inc(long)} are thread-safe
 * atomic updates; the flush emits the running total and the window's delta through the
 * instrument's reused {@link CounterMetric}.
 */
public class CounterInstrument extends Instrument {

    private final CounterMetric metric;

    // Atomic so shared owners can record while a monitor thread flushes the window.
    private final AtomicLong count = new AtomicLong();
    private final AtomicLong lastFlushedCount = new AtomicLong();

    // Registration-time switch; false enforces the Prometheus monotonic-counter contract.
    private boolean allowNegative;

    CounterInstrument(String source, String name) {
        metric = new CounterMetric().source(source).name(name);
    }

    @Override
    Metric<?> metric() {
        return metric;
    }

    /**
     * Permits negative deltas in {@link #inc(long)}. By default they throw, because a counter
     * is exported as a Prometheus counter, whose contract is monotonic - a decrease is read
     * by every scraper as a process restart and corrupts {@code rate()} queries. Call this at
     * registration only for values that are genuinely signed (and expect gauge-like export).
     *
     * @return this instance for chaining
     */
    public CounterInstrument allowNegative() {
        allowNegative = true;
        return this;
    }

    /**
     * Adds a registration-time label to this instrument's metric.
     *
     * @param key   the label key
     * @param value the label value
     * @return this instance for chaining
     * @throws IllegalArgumentException if {@code key} is invalid or duplicate, or {@code value} is {@code null}
     */
    public CounterInstrument label(String key, String value) {
        addLabel(metric, key, value);
        return this;
    }

    /**
     * Sets the registration-time unit of this instrument's metric.
     *
     * @param unit the unit
     * @return this instance for chaining
     */
    public CounterInstrument unit(String unit) {
        metric.unit(unit);
        return this;
    }

    /**
     * Increments the counter by one. Hot path: no allocation; one atomic add.
     */
    public void inc() {
        inc(1);
    }

    /**
     * Increments the counter by {@code n}. Hot path: no allocation; one atomic add.
     * Negative {@code n} throws unless the instrument was registered with
     * {@link #allowNegative()} - counters are exported with the Prometheus monotonic
     * contract, see there.
     *
     * @param n the amount to add
     * @throws IllegalArgumentException if {@code n} is negative and {@link #allowNegative()}
     *                                  was not called
     */
    public void inc(long n) {
        if (n < 0 && !allowNegative)
            throw new IllegalArgumentException(
                    "negative delta " + n + " on monotonic counter '" + metric.name()
                            + "'; register with allowNegative() if this is intended");
        count.addAndGet(n);
    }

    /**
     * Returns the running total.
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
        out.counterMetric(metric.count(c).delta(c - previous)
                .eventTime(eventTime).intervalNs(intervalNs));
    }

    @Override
    void rollWindow() {
        lastFlushedCount.set(count.get());
    }
}
