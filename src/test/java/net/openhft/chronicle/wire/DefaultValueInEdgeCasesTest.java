/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies behaviour when fields are absent vs explicitly null.
 */
class DefaultValueInEdgeCasesTest extends WireTestCommon {

    static class WithDefaults extends SelfDescribingMarshallable {
        int i = 7;
        String s = "d";
    }

    @Test
    void absentFieldsPreserveDefaults() {
        String doc = "!" + WithDefaults.class.getName() + " { i: 10 }";
        WithDefaults wd = WireType.TEXT.fromString(WithDefaults.class, doc);
        assertEquals(10, wd.i);
        assertEquals("d", wd.s); // default preserved because 's' absent
    }

    @Test
    void explicitNullOverridesWrapper() {
        // Use YAML null literal to ensure a true null is parsed.
        String doc = "!" + WithDefaults.class.getName() + " { s: !!null }";
        WithDefaults wd = WireType.TEXT.fromString(WithDefaults.class, doc);
        assertNull(wd.s);
        assertEquals(7, wd.i);
    }
}
