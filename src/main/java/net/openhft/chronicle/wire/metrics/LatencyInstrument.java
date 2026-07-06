/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

import net.openhft.chronicle.core.util.Histogram;

import java.util.Arrays;

/**
 * A latency instrument: {@link #record(long)} samples into a {@link Histogram}
 * (Chronicle-Core's zero-allocation histogram, the same one JLBH uses). Recording and
 * window rollover are synchronized because the underlying histogram is mutable; the flush
 * copies the histogram's state into the instrument's reused {@link MetricHistogram} and
 * rolls the window <em>under</em> that monitor (bounded work, no I/O), then writes to the
 * sink <em>outside</em> it - a recording thread (e.g. an appender committing an excerpt)
 * is never blocked behind a slow sink. Flushing is single-flusher (the registry's flush
 * cadence); concurrent flushers would race on the reused DTO.
 */
public class LatencyInstrument extends Instrument {

    private Histogram[] histograms;
    private HistogramMetric[] metrics;

    // Window sum of recorded durations. Core's Histogram does not track a sum (and its mean
    // is not exposed), so it is maintained here under the same monitor as the histogram.
    private long[] sums;

    // Optional display/storage tiers. Null preserves the original emit-on-every-flush behaviour.
    private long[] windowIntervalsNs;
    private long[] elapsedNs;

    // Reused single-flusher scratch state; see the class javadoc.
    private boolean[] emit;

    LatencyInstrument(String source, String name) {
        this(source, name, new Histogram());
    }

    LatencyInstrument(String source, String name, Histogram histogram) {
        this.histograms = new Histogram[]{histogram};
        this.metrics = new HistogramMetric[]{newMetric(source, name)};
        this.sums = new long[1];
        this.emit = new boolean[1];
    }

    @Override
    Metric<?> metric() {
        return metrics[0];
    }

    /**
     * Adds a registration-time label to this instrument's metric.
     *
     * @param key   the label key
     * @param value the label value
     * @return this instance for chaining
     * @throws IllegalArgumentException if {@code key} is invalid or duplicate, or {@code value} is {@code null}
     */
    public synchronized LatencyInstrument label(String key, String value) {
        addLabel(metrics[0], key, value);
        copyPrimaryIdentityToOtherWindows();
        return this;
    }

    /**
     * Sets the registration-time unit of this instrument's metric.
     *
     * @param unit the unit
     * @return this instance for chaining
     */
    public synchronized LatencyInstrument unit(String unit) {
        metrics[0].unit(unit);
        copyPrimaryIdentityToOtherWindows();
        return this;
    }

    /**
     * Configures this latency instrument to publish independent histogram windows at the
     * supplied intervals. Each recorded duration is sampled into every configured window; a
     * window is cleared only after that window has been emitted. The owner should continue
     * flushing on the shortest interval and configure longer intervals as multiples of that
     * cadence. Call during registration, before recording starts.
     *
     * @param intervalsNs positive window intervals, in nanoseconds
     * @return this instance for chaining
     * @throws IllegalArgumentException if no interval is supplied, or any interval is
     *                                  non-positive or duplicated
     */
    public synchronized LatencyInstrument windowIntervalsNs(long... intervalsNs) {
        long[] intervals = normaliseWindowIntervals(intervalsNs);
        HistogramMetric primary = metrics[0];
        Histogram[] newHistograms = new Histogram[intervals.length];
        HistogramMetric[] newMetrics = new HistogramMetric[intervals.length];
        for (int i = 0; i < intervals.length; i++) {
            newHistograms[i] = new Histogram();
            newMetrics[i] = newMetric(primary.source(), primary.name())
                    .unit(primary.unit())
                    .labelsUnchecked(primary.labels());
        }
        this.histograms = newHistograms;
        this.metrics = newMetrics;
        this.sums = new long[intervals.length];
        this.windowIntervalsNs = intervals;
        this.elapsedNs = new long[intervals.length];
        this.emit = new boolean[intervals.length];
        return this;
    }

    /**
     * Records one duration. Hot path: a few nanoseconds, no allocation.
     *
     * @param durationNs the duration in nanoseconds
     */
    public synchronized void record(long durationNs) {
        if (durationNs < 0)
            throw new IllegalArgumentException(
                    "negative latency " + durationNs + " for histogram '" + metrics[0].name() + "'");
        for (int i = 0; i < histograms.length; i++) {
            histograms[i].sampleNanos(durationNs);
            sums[i] += durationNs;
        }
    }

    /**
     * Returns the underlying histogram; exposed for tests and owner-flush wiring. Note that
     * the window {@code sum} is maintained by {@link #record(long)}, not by the histogram -
     * samples fed to the histogram directly are not reflected in the emitted sum.
     *
     * @return the histogram samples accumulate into
     */
    public synchronized Histogram histogram() {
        return histograms[0];
    }

    @Override
    void flushTo(MetricsOut out, long eventTime, long intervalNs) {
        synchronized (this) {
            if (windowIntervalsNs == null) {
                snapshotAndReset(0, eventTime, intervalNs);
                emit[0] = true;
            } else {
                for (int i = 0; i < windowIntervalsNs.length; i++) {
                    long elapsed = addSaturated(elapsedNs[i], intervalNs);
                    elapsedNs[i] = elapsed;
                    if (elapsed >= windowIntervalsNs[i]) {
                        snapshotAndReset(i, eventTime, elapsed);
                        elapsedNs[i] = 0;
                        emit[i] = true;
                    } else {
                        emit[i] = false;
                    }
                }
            }
        }
        // sink write outside the monitor: record() must never wait on sink I/O
        for (int i = 0; i < metrics.length; i++)
            if (emit[i])
                out.histogramMetric(metrics[i]);
    }

    @Override
    synchronized void rollWindow() {
        for (int i = 0; i < histograms.length; i++) {
            histograms[i].reset();
            sums[i] = 0;
            emit[i] = false;
        }
        if (elapsedNs != null)
            Arrays.fill(elapsedNs, 0);
    }

    private static HistogramMetric newMetric(String source, String name) {
        return new HistogramMetric().histogram(new MetricHistogram())
                .source(source).name(name);
    }

    private static long[] normaliseWindowIntervals(long... intervalsNs) {
        if (intervalsNs == null || intervalsNs.length == 0)
            throw new IllegalArgumentException("at least one latency window interval is required");
        long[] intervals = intervalsNs.clone();
        Arrays.sort(intervals);
        for (int i = 0; i < intervals.length; i++) {
            if (intervals[i] <= 0)
                throw new IllegalArgumentException("latency window interval must be positive: " + intervals[i]);
            if (i > 0 && intervals[i] == intervals[i - 1])
                throw new IllegalArgumentException("duplicate latency window interval: " + intervals[i]);
        }
        return intervals;
    }

    private static long addSaturated(long a, long b) {
        long result = a + b;
        return a > 0 && b > 0 && result < 0 ? Long.MAX_VALUE : result;
    }

    private void snapshotAndReset(int index, long eventTime, long intervalNs) {
        metrics[index].histogram().sampleFrom(histograms[index]).sum(sums[index]);
        histograms[index].reset();
        sums[index] = 0;
        metrics[index].eventTime(eventTime).intervalNs(intervalNs);
    }

    private void copyPrimaryIdentityToOtherWindows() {
        HistogramMetric primary = metrics[0];
        for (int i = 1; i < metrics.length; i++) {
            metrics[i].source(primary.source())
                    .name(primary.name())
                    .unit(primary.unit())
                    .labelsUnchecked(primary.labels());
        }
    }
}
