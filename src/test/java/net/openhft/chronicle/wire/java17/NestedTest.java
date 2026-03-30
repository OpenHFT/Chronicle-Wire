/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.java17;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NestedTest class extending from the common wire test base.
 */
class NestedTest extends WireTestCommon {

    // Test for adding an alias for the Group class and asserting its structure
    @Test
    void mini() {

        // Add an alias for the Group class to the ClassAliasPool
        ClassAliasPool.CLASS_ALIASES.addAlias(Group.class);

        // Create a new Field instance
        Field field = new Field();

        // Create a new Group instance passing the created field
        Group g = new Group(field);

        // Set the "parent" field as "NO" required
        field.required("parent", Required.NO);

        // Assert the structure and formatting of the Group object
        assertEquals("!Group {\n" +
                "  field: {\n" +
                "    required: {\n" +
                "      parent: NO\n" +
                "    }\n" +
                "  }\n" +
                "}\n", g.toString());
    }
}
