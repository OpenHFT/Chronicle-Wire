/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

import java.util.ArrayList;
import java.util.List;

/**
 * Test helper: captures every event as a deep copy (the emitters reuse their DTOs) plus the
 * emitting method's name, so tests can assert method-name discrimination and field values.
 */
class CapturingMetricsOut implements MetricsOut {
    final List<String> methods = new ArrayList<>();
    final List<Metric<?>> metrics = new ArrayList<>();

    @Override
    public void counterMetric(CounterMetric metric) {
        methods.add("counterMetric");
        metrics.add(metric.deepCopy());
    }

    @Override
    public void gaugeMetric(GaugeMetric metric) {
        methods.add("gaugeMetric");
        metrics.add(metric.deepCopy());
    }

    @Override
    public void histogramMetric(HistogramMetric metric) {
        methods.add("histogramMetric");
        metrics.add(metric.deepCopy());
    }

    @Override
    public void rateMetric(RateMetric metric) {
        methods.add("rateMetric");
        metrics.add(metric.deepCopy());
    }

    @Override
    public void pointEvent(PointEvent metric) {
        methods.add("pointEvent");
        metrics.add(metric.deepCopy());
    }

    int size() {
        return methods.size();
    }

    @SuppressWarnings("unchecked")
    <M extends Metric<M>> M metric(int index) {
        return (M) metrics.get(index);
    }
}
