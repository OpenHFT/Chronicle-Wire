/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.metrics;

import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Longest-dotted-prefix precedence, root fallback and the configurable default.
 */
public class SourceSelectionTest extends WireTestCommon {

    @Test
    public void longestPrefixWins() {
        Map<String, Boolean> settings = new LinkedHashMap<>();
        settings.put("", true);
        settings.put("chronicle.queue.tailer", false);
        settings.put("chronicle.fix.session", true);
        settings.put("chronicle.fix", false);
        SourceSelection selection = new SourceSelection(settings);

        // exact match
        assertFalse(selection.enabled("chronicle.queue.tailer"));
        // longer source, matched by its longest configured prefix
        assertFalse(selection.enabled("chronicle.queue.tailer.index"));
        // more specific prefix beats the shorter one
        assertTrue(selection.enabled("chronicle.fix.session"));
        assertTrue(selection.enabled("chronicle.fix.session.LSE"));
        assertFalse(selection.enabled("chronicle.fix.router"));
        assertFalse(selection.enabled("chronicle.fix"));
        // unmatched below the root falls back to ""
        assertTrue(selection.enabled("chronicle.queue.appender"));
        assertTrue(selection.enabled("some.other.library"));
    }

    @Test
    public void defaultAppliesWhenNoRootConfigured() {
        Map<String, Boolean> settings = new LinkedHashMap<>();
        settings.put("chronicle.fix", true);

        SourceSelection offByDefault = new SourceSelection(settings, false);
        assertTrue(offByDefault.enabled("chronicle.fix.session"));
        assertFalse(offByDefault.enabled("chronicle.queue.appender"));
        assertFalse(offByDefault.enabled("nodots"));

        SourceSelection onByDefault = new SourceSelection(settings);
        assertTrue(onByDefault.enabled("chronicle.queue.appender"));
        assertTrue(onByDefault.enabled("nodots"));
    }

    @Test
    public void rootOnlyConfiguration() {
        Map<String, Boolean> settings = new LinkedHashMap<>();
        settings.put("", false);
        SourceSelection selection = new SourceSelection(settings, true);
        assertFalse(selection.enabled("anything.at.all"));
        assertFalse(selection.enabled(""));
    }
}
