/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Verifies behaviour when fields are absent vs explicitly null.
 */
public class DefaultValueInEdgeCasesTest extends WireTestCommon {

    public static class WithDefaults extends SelfDescribingMarshallable {
        final int i = 7;
        final String s = "d";
    }

    @Test
    public void absentFieldsPreserveDefaults() {
        String doc = "!" + WithDefaults.class.getName() + " { i: 10 }";
        WithDefaults wd = WireType.TEXT.fromString(WithDefaults.class, doc);
        assertEquals(10, wd.i);
        assertEquals("d", wd.s); // default preserved because 's' absent
    }

    @Test
    public void explicitNullOverridesWrapper() {
        // Use YAML null literal to ensure a true null is parsed.
        String doc = "!" + WithDefaults.class.getName() + " { s: !!null }";
        WithDefaults wd = WireType.TEXT.fromString(WithDefaults.class, doc);
        assertNull(wd.s);
        assertEquals(7, wd.i);
    }
}
