/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

/**
 * A gauge instrument: {@link #set(double)} is a plain-field store intended to be
 * thread-confined to the recording thread; the flush emits the latest value through the
 * instrument's reused {@link GaugeMetric}.
 */
public class GaugeInstrument extends Instrument {

    private final GaugeMetric metric;

    // Written by the recording thread with plain stores; read at flush cadence.
    private double value;

    GaugeInstrument(String source, String name) {
        metric = new GaugeMetric().source(source).name(name);
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
    public GaugeInstrument label(String key, String value) {
        metric.label(key, value);
        return this;
    }

    /**
     * Sets the registration-time unit of this instrument's metric.
     *
     * @param unit the unit
     * @return this instance for chaining
     */
    public GaugeInstrument unit(String unit) {
        metric.unit(unit);
        return this;
    }

    /**
     * Records the current reading. Hot path: no allocation, no volatile store.
     *
     * @param value the reading
     */
    public void set(double value) {
        this.value = value;
    }

    /**
     * Returns the latest reading.
     *
     * @return the value
     */
    public double value() {
        return value;
    }

    @Override
    void flushTo(MetricsOut out, long eventTime, long intervalNs) {
        out.gaugeMetric(metric.value(value).eventTime(eventTime).intervalNs(intervalNs));
    }
}
