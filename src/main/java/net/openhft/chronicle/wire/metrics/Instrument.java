/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

/**
 * The base of the tier-2 recording instruments ({@link CounterInstrument},
 * {@link GaugeInstrument}, {@link LatencyInstrument}, {@link RateInstrument}). Each instrument
 * is created once at registration by a {@link MetricsRegistry}, owns its reused concrete
 * {@link Metric} DTO with the identity fields set once, and accumulates into plain fields on
 * the hot path - flushing overwrites only the value fields and re-emits the same instance.
 * <p>
 * {@link #close()} deregisters the instrument from its registry, after which it is no longer
 * flushed; recording into a closed instrument remains safe (plain field updates) but is never
 * emitted. Closing is a cold path and may allocate.
 * <p>
 * The instrument set is closed: subclassing outside this package is unsupported.
 */
public abstract class Instrument implements AutoCloseable {

    // The owning registry while registered; null once closed. Guarded by the registry's lock.
    private MetricsRegistry registry;

    // The registry's identity-dedup key; set once at registration.
    String identityKey;

    /**
     * Package-private: the instrument set is closed, see the class javadoc.
     */
    Instrument() {
    }

    /**
     * Returns this instrument's reused metric DTO; identity fields only, for registration
     * wiring and identity checks.
     *
     * @return the reused DTO
     */
    abstract Metric<?> metric();

    /**
     * Emits this instrument's current window through the matching {@link MetricsOut} method,
     * reusing the instrument's DTO. Called by {@link MetricsRegistry#flush(MetricsOut, long, long)}.
     *
     * @param out        the destination
     * @param eventTime  the snapshot time, in ServicesTimestampLongConverter units
     * @param intervalNs the aggregation window ending at {@code eventTime}, in nanoseconds
     */
    abstract void flushTo(MetricsOut out, long eventTime, long intervalNs);

    void registered(MetricsRegistry registry, String identityKey) {
        this.registry = registry;
        this.identityKey = identityKey;
    }

    /**
     * Deregisters this instrument from its registry: it is removed from the flush loop and
     * from identity dedup (so re-registering the same name and labels creates a fresh
     * instrument). Idempotent; never throws. Cold path - removal may allocate.
     */
    @Override
    public void close() {
        MetricsRegistry registry = this.registry;
        if (registry != null)
            registry.remove(this);
    }

    // Called by the registry, under its lock, when this instrument is removed.
    void deregistered() {
        this.registry = null;
    }
}
