/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

/**
 * A monotonic counter snapshot: {@code count} is the running total (survives gaps and restarts,
 * Prometheus-style), {@code delta} is the increase over the window {@code intervalNs} - carrying
 * both means neither the consumer nor a fixture has to reconstruct the other statefully.
 * <p>
 * Part of the closed {@link Metric} hierarchy; subclassing is unsupported. The public no-arg
 * constructor exists for Wire deserialization.
 */
public class CounterMetric extends Metric<CounterMetric> {

    // Running total since the instrument was registered
    private long count;

    // Increase over the window intervalNs
    private long delta;

    /**
     * For Wire deserialization and instrument registration; subclassing is unsupported.
     */
    public CounterMetric() {
    }

    /**
     * Returns the running total.
     *
     * @return the count
     */
    public long count() {
        return count;
    }

    /**
     * Sets the running total.
     *
     * @param count the count to set
     * @return this instance for chaining
     */
    public CounterMetric count(long count) {
        this.count = count;
        return this;
    }

    /**
     * Returns the increase over the window {@code intervalNs}.
     *
     * @return the delta
     */
    public long delta() {
        return delta;
    }

    /**
     * Sets the increase over the window {@code intervalNs}.
     *
     * @param delta the delta to set
     * @return this instance for chaining
     */
    public CounterMetric delta(long delta) {
        this.delta = delta;
        return this;
    }
}
