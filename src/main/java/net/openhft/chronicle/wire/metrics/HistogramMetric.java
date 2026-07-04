/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

/**
 * A latency/size distribution snapshot over the window {@code intervalNs}, carried as an
 * embedded {@link MetricHistogram} which is reused per instrument - the flush path overwrites
 * it in place and never allocates.
 * <p>
 * Part of the closed {@link Metric} hierarchy; subclassing is unsupported. The public no-arg
 * constructor exists for Wire deserialization.
 */
public class HistogramMetric extends Metric<HistogramMetric> {

    // The reused distribution snapshot; set once at registration
    private MetricHistogram histogram;

    /**
     * For Wire deserialization and instrument registration; subclassing is unsupported.
     */
    public HistogramMetric() {
    }

    /**
     * Returns the distribution snapshot.
     *
     * @return the histogram, or {@code null} if not set
     */
    public MetricHistogram histogram() {
        return histogram;
    }

    /**
     * Sets the distribution snapshot; called once at registration with the instrument's
     * reused {@link MetricHistogram}.
     *
     * @param histogram the histogram to embed
     * @return this instance for chaining
     */
    public HistogramMetric histogram(MetricHistogram histogram) {
        this.histogram = histogram;
        return this;
    }
}
