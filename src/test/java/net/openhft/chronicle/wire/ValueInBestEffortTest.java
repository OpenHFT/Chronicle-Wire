/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ValueInBestEffortTest extends WireTestCommon {

    private static final String YAML = "value: { foo: bar }";

    @Test
    @DisplayName("Strict mode returns text for type mismatch")
    public void strictModeReturnsNullOnTypeMismatch() {
        TextWire wire = TextWire.from(YAML);
        Object result = wire.read("value").object(null, String.class, false);
        // In strict mode, mismatched types are not coerced into a target class;
        // current behaviour returns a textual representation of the mapping.
        assertInstanceOf(String.class, result, "Strict mode should return text when type mismatches");
        String s = (String) result;
        assertTrue(s.startsWith("{"),
                "Strict mode text should start with '{' but was: " + s);
        assertTrue(s.contains("foo: bar"),
                "Strict mode text should include foo: bar but was: " + s);
        assertTrue(s.endsWith("}"),
                "Strict mode text should end with '}' but was: " + s);
    }

    @Test
    @DisplayName("Best effort mode accepts mismatched mapping types")
    public void bestEffortAllowsMismatchedTypes() {
        TextWire wire = TextWire.from(YAML);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = wire.read("value").object(null, Map.class, true);
        assertEquals("bar", map.get("foo"), "Best effort should read foo as bar");
    }
}
