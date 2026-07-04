/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

import net.openhft.chronicle.wire.BaseEvent;
import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.ServicesTimestampLongConverter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The abstract base of all metric event DTOs, carrying the fields every metric kind shares:
 * identity ({@code source}, {@code name}, {@code unit}, {@code labels}) and time
 * ({@code eventTime} via {@link BaseEvent}, plus {@code intervalNs} - the aggregation window
 * ending at {@code eventTime}, {@code 0} for point events).
 * <p>
 * The hierarchy is deliberately <em>closed</em>: the concrete kinds
 * ({@link CounterMetric}, {@link GaugeMetric}, {@link HistogramMetric}, {@link RateMetric},
 * {@link PointEvent}) are part of the on-queue format contract and downstream tooling must be
 * able to enumerate them. <b>Subclassing outside this package is unsupported</b> (the
 * constructor is package-private); user-defined <em>metrics</em> are instances, not subclasses.
 * <p>
 * Intended use is build-once, mutate-and-re-emit: the identity fields are set once at
 * registration through the fluent, self-typed setters (stable {@code String} references
 * thereafter), and only the value fields plus {@code eventTime}/{@code intervalNs} are
 * overwritten per flush - so the recording and flush paths never allocate.
 *
 * @param <M> the concrete metric type, for fluent self-typed setters
 */
public abstract class Metric<M extends Metric<M>> extends SelfDescribingMarshallable implements BaseEvent<M> {

    // Dotted selection/routing key, e.g. "chronicle.fix.session"
    private String source;

    // Exported name, e.g. chronicle_fix_session_resend_requests_total
    private String name;

    // Optional; normally implied by the name suffix (_ns, _bytes, _total)
    private String unit;

    // Optional; the concatenated form "key=value;key2=value2", built ONCE at registration,
    // referenced forever, never per-event - a single text write on the wire
    private String labels;

    // When this snapshot/event was taken, in ServicesTimestampLongConverter units
    @LongConversion(ServicesTimestampLongConverter.class)
    private long eventTime;

    // Aggregation window ending at eventTime; 0 for point events
    private long intervalNs;

    /**
     * Package-private: the metric hierarchy is closed, see the class javadoc.
     */
    Metric() {
    }

    /**
     * Returns {@code this} typed as the concrete metric type.
     *
     * @return this instance
     */
    @SuppressWarnings("unchecked")
    protected M self() {
        return (M) this;
    }

    /**
     * Returns the dotted source key for this metric, e.g. {@code chronicle.fix.session}.
     *
     * @return the source, or {@code null} if not set
     */
    public String source() {
        return source;
    }

    /**
     * Sets the dotted source key, e.g. {@code chronicle.fix.session}.
     *
     * @param source the source to set
     * @return this instance for chaining
     */
    public M source(String source) {
        this.source = source;
        return self();
    }

    /**
     * Returns the exported metric name, e.g. {@code chronicle_fix_session_resend_requests_total}.
     *
     * @return the name, or {@code null} if not set
     */
    public String name() {
        return name;
    }

    /**
     * Sets the exported metric name, validating it against the Prometheus metric-name rules
     * {@code ^[a-zA-Z_:][a-zA-Z0-9_:]*$}.
     *
     * @param name the name to set
     * @return this instance for chaining
     * @throws IllegalArgumentException if the name is {@code null}, empty, or does not match
     *                                  {@code ^[a-zA-Z_:][a-zA-Z0-9_:]*$}
     */
    public M name(String name) {
        this.name = validateName(name);
        return self();
    }

    /**
     * Returns the optional unit; normally implied by the name suffix ({@code _ns}, {@code _bytes}).
     *
     * @return the unit, or {@code null} if not set
     */
    public String unit() {
        return unit;
    }

    /**
     * Sets the optional unit.
     *
     * @param unit the unit to set
     * @return this instance for chaining
     */
    public M unit(String unit) {
        this.unit = unit;
        return self();
    }

    /**
     * Returns the labels for this metric in their concatenated wire form,
     * {@code "key=value;key2=value2"}, or {@code null} if none were added.
     * <p>
     * Label order is <b>registration order and is the series identity</b>: the same keys
     * appended in a different order produce a different labels string and therefore a
     * different series. Build labels in one canonical order per series.
     *
     * @return the raw labels string, or {@code null}
     */
    public String labels() {
        return labels;
    }

    /**
     * Sets the labels in their concatenated wire form, {@code "key=value;key2=value2"},
     * validating the whole string: each key must match {@code ^[a-zA-Z_][a-zA-Z0-9_]*$},
     * each value must not contain {@code '='} or {@code ';'} (blank values are allowed),
     * and duplicate keys are rejected. Prefer {@link #label(String, String)} when building
     * labels one at a time.
     * <p>
     * The pair order in the string <b>is the series identity</b> - see {@link #labels()}.
     *
     * @param labels the raw labels string, or {@code null} for none
     * @return this instance for chaining
     * @throws IllegalArgumentException if the string is not a valid concatenated labels form
     */
    public M labels(String labels) {
        this.labels = validateLabels(labels);
        return self();
    }

    /**
     * Sets the labels without validation. For Wire deserialization paths only, where the
     * string was validated when the event was produced; all user-facing paths must go
     * through {@link #labels(String)} or {@link #label(String, String)}.
     *
     * @param labels the raw labels string, or {@code null} for none
     * @return this instance for chaining
     */
    M labelsUnchecked(String labels) {
        this.labels = labels;
        return self();
    }

    /**
     * Appends a label as {@code key=value} to the concatenated labels string,
     * {@code ';'}-separated from any labels already present. Labels are registration-time
     * identity: call this when the instrument is built (registration may allocate), never
     * on the recording or flush path - per event the prebuilt string is one text write.
     * <p>
     * Label order is <b>registration order and is the series identity</b>: appending the
     * same keys in a different order produces a different series - see {@link #labels()}.
     *
     * @param key   the label key; must match {@code ^[a-zA-Z_][a-zA-Z0-9_]*$} and not
     *              already be present
     * @param value the label value; must not be {@code null} and must not contain
     *              {@code '='} or {@code ';'} (blank is allowed)
     * @return this instance for chaining
     * @throws IllegalArgumentException if the key is invalid or a duplicate, or the value
     *                                  is {@code null} or contains {@code '='} or {@code ';'}
     */
    public M label(String key, String value) {
        validateLabelKey(key);
        validateLabelValue(key, value);
        if (containsLabelKey(labels, key))
            throw new IllegalArgumentException("duplicate label key '" + key + "' in: " + labels);
        StringBuilder sb = new StringBuilder(
                (labels == null ? 0 : labels.length() + 1) + key.length() + 1 + value.length());
        if (labels != null)
            sb.append(labels).append(';');
        sb.append(key).append('=').append(value);
        labels = sb.toString();
        return self();
    }

    /**
     * Validates a metric name against the Prometheus rules {@code ^[a-zA-Z_:][a-zA-Z0-9_:]*$}.
     *
     * @param name the name to validate
     * @return the name, unchanged
     * @throws IllegalArgumentException if the name is invalid
     */
    static String validateName(String name) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("metric name must not be null or empty, was: " + name);
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            boolean ok = (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_' || ch == ':'
                    || (i > 0 && ch >= '0' && ch <= '9');
            if (!ok)
                throw new IllegalArgumentException(
                        "metric name must match ^[a-zA-Z_:][a-zA-Z0-9_:]*$, was: " + name);
        }
        return name;
    }

    static void validateLabelKey(String key) {
        if (key == null || key.isEmpty())
            throw new IllegalArgumentException("label key must not be null or empty, was: " + key);
        for (int i = 0; i < key.length(); i++) {
            char ch = key.charAt(i);
            boolean ok = (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_'
                    || (i > 0 && ch >= '0' && ch <= '9');
            if (!ok)
                throw new IllegalArgumentException(
                        "label key must match ^[a-zA-Z_][a-zA-Z0-9_]*$, was: " + key);
        }
    }

    static void validateLabelValue(String key, String value) {
        if (value == null)
            throw new IllegalArgumentException("label value for key '" + key + "' must not be null");
        if (value.indexOf('=') >= 0 || value.indexOf(';') >= 0)
            throw new IllegalArgumentException(
                    "label value for key '" + key + "' must not contain '=' or ';', was: " + value);
    }

    /**
     * Validates a whole concatenated labels string by parsing its {@code key=value} pairs
     * and applying the same rules as {@link #label(String, String)}, including duplicate
     * key rejection.
     *
     * @param labels the raw labels string, or {@code null} for none
     * @return the labels string, unchanged
     * @throws IllegalArgumentException if any pair is malformed, any key or value is
     *                                  invalid, or a key appears twice
     */
    static String validateLabels(String labels) {
        if (labels == null)
            return null;
        int length = labels.length();
        if (length == 0)
            throw new IllegalArgumentException("labels must be null or non-empty, was: \"\"");
        int start = 0;
        while (start <= length) {
            int end = labels.indexOf(';', start);
            if (end < 0)
                end = length;
            int eq = labels.indexOf('=', start);
            if (eq <= start || eq >= end)
                throw new IllegalArgumentException(
                        "labels must be key=value pairs separated by ';', was: " + labels);
            String key = labels.substring(start, eq);
            validateLabelKey(key);
            validateLabelValue(key, labels.substring(eq + 1, end));
            if (start > 0 && containsLabelKey(labels.substring(0, start - 1), key))
                throw new IllegalArgumentException("duplicate label key '" + key + "' in: " + labels);
            start = end + 1;
        }
        return labels;
    }

    // True if the concatenated labels string already contains the key as a whole key.
    private static boolean containsLabelKey(String labels, String key) {
        if (labels == null)
            return false;
        int from = 0;
        for (; ; ) {
            int at = labels.indexOf(key, from);
            if (at < 0)
                return false;
            boolean startsPair = at == 0 || labels.charAt(at - 1) == ';';
            int after = at + key.length();
            if (startsPair && after < labels.length() && labels.charAt(after) == '=')
                return true;
            from = at + 1;
        }
    }

    /**
     * Decodes the concatenated labels string into a new, insertion-ordered map.
     * <b>Allocates</b> a {@link LinkedHashMap} (and entry strings) on every call - this is
     * for gateways splitting a series once at export time, and for tests; never call it on
     * the recording or flush path.
     *
     * @return a new map of the labels in registration order; empty when no labels are set
     */
    public Map<String, String> decodeLabels() {
        Map<String, String> map = new LinkedHashMap<>();
        if (labels == null)
            return map;
        int start = 0;
        int length = labels.length();
        while (start < length) {
            int end = labels.indexOf(';', start);
            if (end < 0)
                end = length;
            int eq = labels.indexOf('=', start);
            if (eq > start && eq < end)
                map.put(labels.substring(start, eq), labels.substring(eq + 1, end));
            start = end + 1;
        }
        return map;
    }

    /**
     * Returns the time this snapshot/event was taken, in
     * {@link ServicesTimestampLongConverter} units.
     *
     * @return the event time
     */
    @Override
    public long eventTime() {
        return eventTime;
    }

    /**
     * Sets the time this snapshot/event was taken.
     *
     * @param eventTime the event time, in {@link ServicesTimestampLongConverter} units
     * @return this instance for chaining
     */
    @Override
    public M eventTime(long eventTime) {
        this.eventTime = eventTime;
        return self();
    }

    /**
     * Returns the aggregation window ending at {@link #eventTime()}, in nanoseconds;
     * {@code 0} for point events.
     *
     * @return the interval in nanoseconds
     */
    public long intervalNs() {
        return intervalNs;
    }

    /**
     * Sets the aggregation window ending at {@link #eventTime()}.
     *
     * @param intervalNs the interval in nanoseconds; {@code 0} for point events
     * @return this instance for chaining
     */
    public M intervalNs(long intervalNs) {
        this.intervalNs = intervalNs;
        return self();
    }
}
