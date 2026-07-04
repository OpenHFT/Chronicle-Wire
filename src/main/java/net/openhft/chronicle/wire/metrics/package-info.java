/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
/**
 * The open-source metrics API: a small, allocation-free, method-writer-friendly schema for
 * publishing metrics as self-describing Wire events.
 * <p>
 * Tier 1 is {@link net.openhft.chronicle.wire.metrics.MetricsOut} - one method per metric
 * kind, each taking that kind's reused DTO from the closed
 * {@link net.openhft.chronicle.wire.metrics.Metric} hierarchy, so the method name is the
 * on-wire discriminator. Tier 2 is
 * {@link net.openhft.chronicle.wire.metrics.MetricsRegistry} and its instruments - what
 * product code touches on the hot path: pre-registered, allocation-free recorders drained by
 * a host-scheduled flusher.
 * <p>
 * Acquisition is source-keyed via {@link net.openhft.chronicle.wire.metrics.Metrics}, selected
 * like log levels through {@link net.openhft.chronicle.wire.metrics.SourceSelection}, and
 * bound process-wide through {@link net.openhft.chronicle.wire.metrics.MetricsBinding};
 * transports (queue binding, capture, exporters) live with their own products - this package
 * has no dependency beyond Wire itself.
 * <p>
 * The authoritative registry of every metric the portfolio emits (names, kinds, labels,
 * cadences) and the cardinality rules are in {@code docs/metrics-catalogue.adoc}; binding
 * installation, startup-only vs runtime-toggleable integrations and the metrics-queue
 * topology are in {@code docs/metrics-integration.adoc} (both in this repository).
 */
package net.openhft.chronicle.wire.metrics;
