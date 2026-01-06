/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WireKeyTest extends WireTestCommon {

    @Test
    @DisplayName("toCode handles numeric, numeric fallback, and text cases")
    void toCodeHandlesNumericAndText() {
        assertEquals(123, WireKey.toCode("123"), "WireKey parses numeric text to integer code");

        String numericFallback = "9x";
        assertEquals(numericFallback.hashCode(), WireKey.toCode(numericFallback),
                "WireKey uses hash code when numeric parsing fails");

        String text = "alpha";
        assertEquals(text.hashCode(), WireKey.toCode(text), "WireKey uses hash code for non-numeric text");
    }

    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("checkKeys rejects duplicate key codes in arrays")
    void checkKeysRejectsDuplicateCodes() {
        WireKey[] keys = {
                new NamedKey("alpha", 7, null),
                new NamedKey("beta", 7, null)
        };

        AssertionError error = assertThrows(AssertionError.class, () -> WireKey.checkKeys(keys),
                "WireKey.checkKeys should reject duplicate key codes");
        assertTrue(error.getMessage().contains("have the same code"),
                "WireKey duplicate message includes code detail");
    }

    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("checkKeys accepts unique key codes in arrays")
    void checkKeysAcceptsUniqueCodes() {
        WireKey[] keys = {
                new NamedKey("alpha", 1, null),
                new NamedKey("beta", 2, null)
        };

        assertTrue(WireKey.checkKeys(keys), "WireKey.checkKeys should accept unique key codes");
    }

    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("type uses default value when available")
    void typeUsesDefaultValue() {
        NamedKey withDefault = new NamedKey("alpha", 1, "value");
        assertEquals(String.class, withDefault.type(), "WireKey type reflects default value class");

        NamedKey withoutDefault = new NamedKey("beta", 2, null);
        assertEquals(Void.class, withoutDefault.type(), "WireKey type returns Void when default value is null");
    }

    @Test
    @DisplayName("contentEquals compares the string form of the key")
    void contentEqualsComparesStringForms() {
        NamedKey key = new NamedKey("alpha", 1, null);

        assertTrue(key.contentEquals("alpha"), "WireKey.contentEquals should match the name string");
        assertFalse(key.contentEquals("beta"), "WireKey.contentEquals should detect mismatched name values");
    }

    private static final class NamedKey implements WireKey {
        private final String name;
        private final int code;
        private final Object defaultValue;

        private NamedKey(String name, int code, Object defaultValue) {
            this.name = name;
            this.code = code;
            this.defaultValue = defaultValue;
        }

        @Override
        public CharSequence name() {
            return name;
        }

        @Override
        public int code() {
            return code;
        }

        @Override
        public Object defaultValue() {
            return defaultValue;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
