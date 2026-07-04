/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Log-level-style source selection: dotted hierarchical source names are switched on or off by
 * the <em>longest matching dotted prefix</em>, exactly like logger configuration but with
 * on/off instead of levels. The empty key {@code ""} configures the root; sources matching no
 * key fall back to the configurable default.
 * <p>
 * <b>Default-on:</b> once a binding is installed, sources default to <em>enabled</em> unless
 * configured off - products ship their metrics on by default because recording is designed to
 * be ignorable, and operators switch sources off (or flip the default) in configuration. To
 * change the default, configure the root:
 * <pre>{@code
 * metrics: {
 *   sources: {
 *     "": off,                        # flip the default: everything off ...
 *     "chronicle.fix.session": on     # ... except what is explicitly enabled
 *   }
 * }
 * }</pre>
 * <p>
 * Immutable once constructed; lookups do not allocate for sources that are exact keys, and
 * are acquisition-time only in any case.
 */
public class SourceSelection {

    private final Map<String, Boolean> settings;
    private final boolean defaultEnabled;

    /**
     * Creates a selection from prefix settings, defaulting unmatched sources to enabled
     * (products ship on by default - recording is designed to be ignorable).
     *
     * @param settings dotted-prefix to on/off; copied
     */
    public SourceSelection(Map<String, Boolean> settings) {
        this(settings, true);
    }

    /**
     * Creates a selection from prefix settings and an explicit default.
     *
     * @param settings       dotted-prefix to on/off; copied
     * @param defaultEnabled the result when neither the source, any dotted prefix of it,
     *                       nor the root {@code ""} is configured
     */
    public SourceSelection(Map<String, Boolean> settings, boolean defaultEnabled) {
        this.settings = new LinkedHashMap<>(settings);
        this.defaultEnabled = defaultEnabled;
    }

    /**
     * Returns whether the given source is enabled, by longest-dotted-prefix lookup:
     * {@code a.b.c} is matched against {@code a.b.c}, then {@code a.b}, then {@code a},
     * then the root {@code ""}, then the default.
     *
     * @param source the dotted source name
     * @return {@code true} if the source is enabled
     */
    public boolean enabled(String source) {
        String s = source;
        for (; ; ) {
            Boolean b = settings.get(s);
            if (b != null)
                return b;
            int i = s.lastIndexOf('.');
            if (i < 0)
                break;
            s = s.substring(0, i);
        }
        Boolean root = settings.get("");
        if (root != null)
            return root;
        return defaultEnabled;
    }
}
