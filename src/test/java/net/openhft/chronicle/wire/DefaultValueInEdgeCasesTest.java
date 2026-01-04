/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression coverage for default values when fields are absent versus explicit null in TextWire parsing.
 */
public class DefaultValueInEdgeCasesTest extends WireTestCommon {

    public static class WithDefaults extends SelfDescribingMarshallable {
        int i = 7;
        String s = "d";
    }

    @Test
    @DisplayName("Preserves defaults when fields are absent")
    public void absentFieldsPreserveDefaults() {
        String doc = "!" + WithDefaults.class.getName() + " { i: 10 }";
        WithDefaults wd = WireType.TEXT.fromString(WithDefaults.class, doc);
        assertEquals(10, wd.i, "explicit i should override default when present");
        assertEquals("d", wd.s, "absent s should keep default value");
    }

    @Test
    @DisplayName("Overrides defaults when explicit null is provided")
    public void explicitNullOverridesWrapper() {
        // Use YAML null literal to ensure a true null is parsed.
        String doc = "!" + WithDefaults.class.getName() + " { s: !!null }";
        WithDefaults wd = WireType.TEXT.fromString(WithDefaults.class, doc);
        assertNull(wd.s, "explicit null should override default string value");
        assertEquals(7, wd.i, "absent i should keep default value");
    }
}
