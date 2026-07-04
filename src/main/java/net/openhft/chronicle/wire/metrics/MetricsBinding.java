/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

/**
 * The process-level SPI installed via {@link Metrics#install(MetricsBinding)}: resolves a
 * dotted source name to the {@link MetricsOut} that source's events should be delivered to.
 * <p>
 * Implementations live with their transports (a queue binding in Chronicle Queue, a capturing
 * binding in test harnesses, a tee, an in-process dev exporter); returning {@code null} - or an
 * {@link net.openhft.chronicle.core.util.IgnoresEverything} handler - for a source disables it.
 * Typically implemented over a {@link SourceSelection} for log-level-style on/off configuration.
 */
public interface MetricsBinding {

    /**
     * Resolves the destination for a source.
     *
     * @param source the dotted source name, e.g. {@code chronicle.queue.replication}
     * @return the destination, or {@code null} to leave the source ignored
     */
    MetricsOut outFor(String source);
}
