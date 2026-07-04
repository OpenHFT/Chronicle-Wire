/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

import net.openhft.chronicle.core.util.Histogram;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;

/**
 * A marshallable snapshot of a {@link Histogram}: the sample {@code count}, {@code min},
 * {@code max} and {@code sum} of the window, the values at a fixed set of
 * {@linkplain #percentileFractions() percentile fractions}, and the {@code worst}
 * observed value.
 * <p>
 * One instance is embedded in each {@link HistogramMetric} at registration; per flush,
 * {@link #sampleFrom(Histogram)} overwrites this instance in place - the percentile array is
 * sized once at construction, so sampling never allocates.
 * <p>
 * {@code min} and {@code max} come from the histogram ({@link Histogram#min()} /
 * {@link Histogram#max()}); {@code sum} does <b>not</b> - core's {@code Histogram} tracks
 * neither a sum nor an exposed mean, so the producing instrument maintains it (one long add
 * per sample in {@link LatencyInstrument#record(long)}, still allocation-free) and sets it via
 * {@link #sum(long)} at flush.
 */
public class MetricHistogram extends SelfDescribingMarshallable {

    // The percentile fractions captured by sampleFrom, in ascending order.
    static final double[] FRACTIONS = {0.5, 0.9, 0.99, 0.999, 0.9999};

    // Number of observations in the window
    private long count;

    // Smallest observation in the window (Histogram.min(), bucket-quantised)
    private double min;

    // Largest observation in the window (Histogram.max(), bucket-quantised)
    private double max;

    // Sum of the window's observations; maintained by the instrument, not the Histogram
    private long sum;

    // Values at FRACTIONS, in the unit the samples were recorded in (normally nanoseconds)
    private double[] percentiles = new double[FRACTIONS.length];

    // The worst (100th percentile) observation
    private double worst;

    /**
     * Creates an empty snapshot; the percentile array is allocated here, once.
     */
    public MetricHistogram() {
    }

    /**
     * Returns the fixed percentile fractions captured by {@link #sampleFrom(Histogram)}.
     * Allocates a defensive copy - registration/consumer-side use only.
     *
     * @return the fractions, in ascending order
     */
    public static double[] percentileFractions() {
        return FRACTIONS.clone();
    }

    /**
     * Copies the current state of the given histogram into this snapshot: {@code count},
     * {@code min}, {@code max}, the percentiles and {@code worst}. {@code sum} is left
     * untouched - the histogram cannot supply it, see the class javadoc; the producing
     * instrument sets it via {@link #sum(long)}.
     * Does not allocate: the percentile array was sized at construction.
     *
     * @param histogram the histogram to sample
     * @return this instance for chaining
     */
    public MetricHistogram sampleFrom(Histogram histogram) {
        count = histogram.totalCount();
        min = histogram.min();
        max = histogram.max();
        for (int i = 0; i < FRACTIONS.length; i++)
            percentiles[i] = histogram.percentile(FRACTIONS[i]);
        worst = histogram.percentile(1.0);
        return this;
    }

    /**
     * Returns the number of observations in the window.
     *
     * @return the count
     */
    public long count() {
        return count;
    }

    /**
     * Returns the smallest observation in the window, bucket-quantised like the percentiles.
     *
     * @return the minimum
     */
    public double min() {
        return min;
    }

    /**
     * Returns the largest observation in the window, bucket-quantised like the percentiles.
     *
     * @return the maximum
     */
    public double max() {
        return max;
    }

    /**
     * Returns the sum of the window's observations, as maintained by the producing
     * instrument - see the class javadoc.
     *
     * @return the sum
     */
    public long sum() {
        return sum;
    }

    /**
     * Sets the sum of the window's observations; called by the producing instrument at
     * flush, after {@link #sampleFrom(Histogram)}.
     *
     * @param sum the sum to set
     * @return this instance for chaining
     */
    public MetricHistogram sum(long sum) {
        this.sum = sum;
        return this;
    }

    /**
     * Returns the value at the i-th of the {@linkplain #percentileFractions() fixed fractions}.
     *
     * @param index the fraction index
     * @return the percentile value
     */
    public double percentile(int index) {
        return percentiles[index];
    }

    /**
     * Returns the worst (100th percentile) observation.
     *
     * @return the worst value
     */
    public double worst() {
        return worst;
    }
}
