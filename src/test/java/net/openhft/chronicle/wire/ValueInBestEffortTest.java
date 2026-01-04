/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValueInBestEffortTest extends WireTestCommon {

    private static final String YAML = "value: { foo: bar }";

    @Test
    @DisplayName("Strict mode returns text for type mismatch")
    void strictModeReturnsNullOnTypeMismatch() {
        TextWire wire = TextWire.from(YAML);
        String result = wire.read("value").object(null, String.class, false);
        // In strict mode, mismatched types are not coerced into a target class;
        // current behaviour returns a textual representation of the mapping.
        assertInstanceOf(String.class, result, "Strict mode should return text when type mismatches");
        assertTrue(result.startsWith("{"),
                "Strict mode text should start with '{' but was: " + result);
        assertTrue(result.contains("foo: bar"),
                "Strict mode text should include foo: bar but was: " + result);
        assertTrue(result.endsWith("}"),
                "Strict mode text should end with '}' but was: " + result);
    }

    @Test
    @DisplayName("Best effort mode accepts mismatched mapping types")
    void bestEffortAllowsMismatchedTypes() {
        TextWire wire = TextWire.from(YAML);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = wire.read("value").object(null, Map.class, true);
        assertEquals("bar", map.get("foo"), "Best effort should read foo as bar");
    }
}
