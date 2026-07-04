/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

/**
 * A low-rate point event (connection state change, failover, sequence reset): a single
 * {@code value} at {@code eventTime}, with {@code intervalNs} staying {@code 0} because a
 * point event has no aggregation window.
 * <p>
 * Part of the closed {@link Metric} hierarchy; subclassing is unsupported. The public no-arg
 * constructor exists for Wire deserialization.
 */
public class PointEvent extends Metric<PointEvent> {

    // The event's value, e.g. 1 for "a reconnect happened"
    private double value;

    /**
     * For Wire deserialization and instrument registration; subclassing is unsupported.
     */
    public PointEvent() {
    }

    /**
     * Returns the event's value.
     *
     * @return the value
     */
    public double value() {
        return value;
    }

    /**
     * Sets the event's value.
     *
     * @param value the value to set
     * @return this instance for chaining
     */
    public PointEvent value(double value) {
        this.value = value;
        return this;
    }
}
