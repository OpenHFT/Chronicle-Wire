/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

/**
 * The metrics wire interface: one method per metric kind, each taking that kind's DTO, so the
 * method name is the on-wire discriminator ({@code counterMetric: { ... }} in YAML), generated
 * method readers can reuse one DTO instance per method, and invalid states (a counter carrying
 * a histogram) are unrepresentable.
 * <p>
 * The method set is deliberately closed, matching the closed {@link Metric} hierarchy: the
 * kinds are part of the on-queue format contract. Consumers that want a single code path
 * should implement {@link Adapter} instead.
 * <p>
 * Implementations must never block and never throw into product code.
 */
public interface MetricsOut {

    /**
     * Emits a monotonic counter snapshot.
     *
     * @param metric the counter snapshot; the instance may be reused by the caller after return
     */
    void counterMetric(CounterMetric metric);

    /**
     * Emits a gauge snapshot.
     *
     * @param metric the gauge snapshot; the instance may be reused by the caller after return
     */
    void gaugeMetric(GaugeMetric metric);

    /**
     * Emits a distribution snapshot.
     *
     * @param metric the histogram snapshot; the instance may be reused by the caller after return
     */
    void histogramMetric(HistogramMetric metric);

    /**
     * Emits a rate snapshot.
     *
     * @param metric the rate snapshot; the instance may be reused by the caller after return
     */
    void rateMetric(RateMetric metric);

    /**
     * Emits a low-rate point event.
     *
     * @param metric the point event; the instance may be reused by the caller after return
     */
    void pointEvent(PointEvent metric);

    /**
     * A convenience funnel for consumers that genuinely want one code path: all five kind
     * methods default to {@link #onMetric(Metric)}. This is a consumer-side adapter only -
     * the wire format keeps a method per kind.
     */
    interface Adapter extends MetricsOut {

        /**
         * Receives every metric kind.
         *
         * @param metric the metric; the instance may be reused by the caller after return
         */
        void onMetric(Metric<?> metric);

        @Override
        default void counterMetric(CounterMetric metric) {
            onMetric(metric);
        }

        @Override
        default void gaugeMetric(GaugeMetric metric) {
            onMetric(metric);
        }

        @Override
        default void histogramMetric(HistogramMetric metric) {
            onMetric(metric);
        }

        @Override
        default void rateMetric(RateMetric metric) {
            onMetric(metric);
        }

        @Override
        default void pointEvent(PointEvent metric) {
            onMetric(metric);
        }
    }
}
