/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

import net.openhft.chronicle.core.util.IgnoresEverything;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * The tier-2 registry for one source: instruments are registered here at startup (allowed to
 * allocate - each builds its reused, concrete-typed {@link Metric} DTO with the identity
 * fields set once), and a flusher - scheduled by the owner, not by this class - drains them
 * with {@link #flush(MetricsOut, long, long)}.
 * <p>
 * Registries are either process-global per source ({@link Metrics#registry(String)}, for
 * simple cases) or independent per owner ({@link Metrics#newRegistry(String)} - one per event
 * loop or service, flushed on that owner's cadence, touching only that owner's instruments).
 * <p>
 * Registration applies <b>identity dedup</b>: registering the same kind, name and labels twice
 * on one registry returns the <em>same</em> instrument instance, never a duplicate series.
 * The identity is the (kind, name, labels) passed to the registration call - labels appended
 * fluently afterwards do not participate, so pass labels at registration when relying on
 * dedup. Metric names are validated against {@code ^[a-zA-Z_:][a-zA-Z0-9_:]*$} at
 * registration.
 * <p>
 * Instruments deregister via {@link Instrument#close()}; {@link #close()} removes all
 * instruments and makes further flushes no-ops (further registrations throw
 * {@link IllegalStateException}). Removal compacts the flat array and may allocate - it is a
 * cold path.
 * <p>
 * The flush path is steady-state allocation-free: an indexed {@code for} loop over a flat
 * {@link Instrument} array - no {@code Iterator}, no streams, no boxing - and each instrument
 * re-emits its reused DTO. Registration is thread-safe; recording is designed to be
 * thread-confined per the design's ownership rules.
 */
public class MetricsRegistry implements AutoCloseable {

    private static final Instrument[] NO_INSTRUMENTS = {};

    private final String source;

    // Identity dedup: (kind, name, labels) -> instrument. Guarded by this; registration only.
    private final Map<String, Instrument> byIdentity = new HashMap<>();

    // Grown only at registration; read with a volatile load per flush.
    private volatile Instrument[] instruments = NO_INSTRUMENTS;

    // Guarded by this.
    private boolean closed;

    MetricsRegistry(String source) {
        this.source = source;
    }

    /**
     * Returns the dotted source name this registry's instruments are registered under.
     *
     * @return the source
     */
    public String source() {
        return source;
    }

    /**
     * Registers a monotonic counter with no labels, or returns the instrument already
     * registered under the same name (identity dedup).
     *
     * @param name the exported metric name, e.g. {@code chronicle_queue_appends_total}
     * @return the instrument for this identity
     * @throws IllegalArgumentException if the name is invalid
     * @throws IllegalStateException    if this registry is closed
     */
    public CounterInstrument counter(String name) {
        return counter(name, null);
    }

    /**
     * Registers a monotonic counter, or returns the instrument already registered under the
     * same name and labels (identity dedup).
     *
     * @param name   the exported metric name, e.g. {@code chronicle_queue_appends_total}
     * @param labels the concatenated labels form {@code "key=value;key2=value2"}, or
     *               {@code null} for none; validated, and part of the dedup identity
     * @return the instrument for this identity
     * @throws IllegalArgumentException if the name or labels are invalid
     * @throws IllegalStateException    if this registry is closed
     */
    public synchronized CounterInstrument counter(String name, String labels) {
        String key = identityKey('c', name, labels);
        Instrument existing = byIdentity.get(key);
        if (existing != null)
            return (CounterInstrument) existing;
        return add(key, new CounterInstrument(source, name), labels);
    }

    /**
     * Registers a gauge with no labels, or returns the instrument already registered under
     * the same name (identity dedup).
     *
     * @param name the exported metric name
     * @return the instrument for this identity
     * @throws IllegalArgumentException if the name is invalid
     * @throws IllegalStateException    if this registry is closed
     */
    public GaugeInstrument gauge(String name) {
        return gauge(name, null);
    }

    /**
     * Registers a gauge, or returns the instrument already registered under the same name and
     * labels (identity dedup).
     *
     * @param name   the exported metric name
     * @param labels the concatenated labels form, or {@code null}; validated, part of the identity
     * @return the instrument for this identity
     * @throws IllegalArgumentException if the name or labels are invalid
     * @throws IllegalStateException    if this registry is closed
     */
    public synchronized GaugeInstrument gauge(String name, String labels) {
        String key = identityKey('g', name, labels);
        Instrument existing = byIdentity.get(key);
        if (existing != null)
            return (GaugeInstrument) existing;
        return add(key, new GaugeInstrument(source, name), labels);
    }

    /**
     * Registers a latency histogram with no labels, or returns the instrument already
     * registered under the same name (identity dedup).
     *
     * @param name the exported metric name, e.g. {@code chronicle_queue_write_latency_ns}
     * @return the instrument for this identity
     * @throws IllegalArgumentException if the name is invalid
     * @throws IllegalStateException    if this registry is closed
     */
    public LatencyInstrument latency(String name) {
        return latency(name, null);
    }

    /**
     * Registers a latency histogram, or returns the instrument already registered under the
     * same name and labels (identity dedup).
     *
     * @param name   the exported metric name, e.g. {@code chronicle_queue_write_latency_ns}
     * @param labels the concatenated labels form, or {@code null}; validated, part of the identity
     * @return the instrument for this identity
     * @throws IllegalArgumentException if the name or labels are invalid
     * @throws IllegalStateException    if this registry is closed
     */
    public synchronized LatencyInstrument latency(String name, String labels) {
        String key = identityKey('h', name, labels);
        Instrument existing = byIdentity.get(key);
        if (existing != null)
            return (LatencyInstrument) existing;
        return add(key, new LatencyInstrument(source, name), labels);
    }

    /**
     * Registers a rate with no labels, or returns the instrument already registered under
     * the same name (identity dedup).
     *
     * @param name the exported metric name
     * @return the instrument for this identity
     * @throws IllegalArgumentException if the name is invalid
     * @throws IllegalStateException    if this registry is closed
     */
    public RateInstrument rate(String name) {
        return rate(name, null);
    }

    /**
     * Registers a rate, or returns the instrument already registered under the same name and
     * labels (identity dedup).
     *
     * @param name   the exported metric name
     * @param labels the concatenated labels form, or {@code null}; validated, part of the identity
     * @return the instrument for this identity
     * @throws IllegalArgumentException if the name or labels are invalid
     * @throws IllegalStateException    if this registry is closed
     */
    public synchronized RateInstrument rate(String name, String labels) {
        String key = identityKey('r', name, labels);
        Instrument existing = byIdentity.get(key);
        if (existing != null)
            return (RateInstrument) existing;
        return add(key, new RateInstrument(source, name), labels);
    }

    // Validates name and labels, then builds the dedup key. Name chars cannot include '|'.
    private static String identityKey(char kind, String name, String labels) {
        Metric.validateName(name);
        Metric.validateLabels(labels);
        return kind + name + '|' + (labels == null ? "" : labels);
    }

    // Callers hold this registry's lock (the public registration methods are synchronized).
    private <I extends Instrument> I add(String key, I instrument, String labels) {
        if (closed)
            throw new IllegalStateException("registry for source '" + source + "' is closed");
        if (labels != null)
            instrument.metric().labelsUnchecked(labels); // validated in identityKey
        Instrument[] instruments = this.instruments;
        instruments = Arrays.copyOf(instruments, instruments.length + 1);
        instruments[instruments.length - 1] = instrument;
        instrument.registered(this, key);
        byIdentity.put(key, instrument);
        this.instruments = instruments;
        return instrument;
    }

    // Called by Instrument.close(); cold path, may allocate.
    synchronized void remove(Instrument instrument) {
        Instrument[] instruments = this.instruments;
        int at = -1;
        for (int i = 0; i < instruments.length; i++)
            if (instruments[i] == instrument)
                at = i;
        if (at < 0)
            return;
        Instrument[] compacted = new Instrument[instruments.length - 1];
        System.arraycopy(instruments, 0, compacted, 0, at);
        System.arraycopy(instruments, at + 1, compacted, at, instruments.length - at - 1);
        byIdentity.remove(instrument.identityKey);
        instrument.deregistered();
        this.instruments = compacted;
    }

    /**
     * Returns whether this registry has been {@linkplain #close() closed}.
     *
     * @return {@code true} once closed
     */
    public synchronized boolean isClosed() {
        return closed;
    }

    /**
     * Removes all instruments and closes the registry: further flushes are no-ops, further
     * registrations throw {@link IllegalStateException}. Idempotent; never throws.
     */
    @Override
    public synchronized void close() {
        if (closed)
            return;
        closed = true;
        Instrument[] instruments = this.instruments;
        for (int i = 0; i < instruments.length; i++)
            instruments[i].deregistered();
        byIdentity.clear();
        this.instruments = NO_INSTRUMENTS;
    }

    /**
     * Emits every registered instrument's current window through the matching
     * {@link MetricsOut} method. Steady-state allocation-free; skips all work when the
     * destination, after unwrapping a {@link ThreadLocalisedMetricsOut} facade, is
     * {@code instanceof} {@link IgnoresEverything}, and is a no-op once this registry is
     * {@linkplain #close() closed}.
     *
     * @param out        the destination
     * @param eventTime  the snapshot time, in ServicesTimestampLongConverter units
     * @param intervalNs the aggregation window ending at {@code eventTime}, in nanoseconds
     */
    public void flush(MetricsOut out, long eventTime, long intervalNs) {
        out = ThreadLocalisedMetricsOut.unwrap(out);
        if (out instanceof IgnoresEverything)
            return;
        final Instrument[] instruments = this.instruments;
        for (int i = 0; i < instruments.length; i++)
            instruments[i].flushTo(out, eventTime, intervalNs);
    }
}
