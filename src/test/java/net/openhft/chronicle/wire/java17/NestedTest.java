/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.java17;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies nested class formatting for alias pool group structures in wire tests.
 */
class NestedTest extends WireTestCommon {

    // Test for adding an alias for the Group class and asserting its structure
    @Test
    @DisplayName("Alias pool should render group structure")
    void mini() {

        // Add an alias for the Group class to the ClassAliasPool
        ClassAliasPool.CLASS_ALIASES.addAlias(Group.class);

        // Create a new Field instance
        Field field = new Field();

        // Create a new Group instance passing the created field
        Group g = new Group(field);

        // Set the "parent" field as "NO" required
        field.required("parent", Required.NO);
        assertSame(field, g.getField(), "Group should retain the supplied field instance");

        // Assert the structure and formatting of the Group object
        assertEquals(
            "!Group {\n" +
            "  field: {\n" +
            "    required: {\n" +
            "      parent: NO\n" +
            "    }\n" +
            "  }\n" +
            "}\n", g.toString(),
            "Group string representation should match expected format"
        );
    }
}
