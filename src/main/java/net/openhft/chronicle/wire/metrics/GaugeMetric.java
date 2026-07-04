/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

/**
 * A gauge snapshot: the current {@code value} of something that can go up and down
 * (queue depth, lag, busy fraction) at {@code eventTime}.
 * <p>
 * Part of the closed {@link Metric} hierarchy; subclassing is unsupported. The public no-arg
 * constructor exists for Wire deserialization.
 */
public class GaugeMetric extends Metric<GaugeMetric> {

    // The gauge reading at eventTime
    private double value;

    /**
     * For Wire deserialization and instrument registration; subclassing is unsupported.
     */
    public GaugeMetric() {
    }

    /**
     * Returns the gauge reading.
     *
     * @return the value
     */
    public double value() {
        return value;
    }

    /**
     * Sets the gauge reading.
     *
     * @param value the value to set
     * @return this instance for chaining
     */
    public GaugeMetric value(double value) {
        this.value = value;
        return this;
    }
}
