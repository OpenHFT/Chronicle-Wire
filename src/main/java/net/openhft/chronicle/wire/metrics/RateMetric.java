/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

/**
 * A rate snapshot: the running {@code count} of occurrences plus the observed
 * {@code perSecond} rate over the window {@code intervalNs}.
 * <p>
 * Part of the closed {@link Metric} hierarchy; subclassing is unsupported. The public no-arg
 * constructor exists for Wire deserialization.
 */
public class RateMetric extends Metric<RateMetric> {

    // Running total of occurrences
    private long count;

    // Occurrences per second over the window intervalNs
    private double perSecond;

    /**
     * For Wire deserialization and instrument registration; subclassing is unsupported.
     */
    public RateMetric() {
    }

    /**
     * Returns the running total of occurrences.
     *
     * @return the count
     */
    public long count() {
        return count;
    }

    /**
     * Sets the running total of occurrences.
     *
     * @param count the count to set
     * @return this instance for chaining
     */
    public RateMetric count(long count) {
        this.count = count;
        return this;
    }

    /**
     * Returns the occurrences per second over the window {@code intervalNs}.
     *
     * @return the per-second rate
     */
    public double perSecond() {
        return perSecond;
    }

    /**
     * Sets the occurrences per second over the window {@code intervalNs}.
     *
     * @param perSecond the per-second rate to set
     * @return this instance for chaining
     */
    public RateMetric perSecond(double perSecond) {
        this.perSecond = perSecond;
        return this;
    }
}
