/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.java17;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * NestedTest class extending from the common wire test base.
 */
public class NestedTest extends WireTestCommon {

    // Test for adding an alias for the Group class and asserting its structure
    @Test
    public void mini() {

        // Add an alias for the Group class to the ClassAliasPool
        ClassAliasPool.CLASS_ALIASES.addAlias(Group.class);

        // Create a new Field instance
        Field field = new Field();

        // Create a new Group instance passing the created field
        Group g = new Group(field);

        // Set the "parent" field as "NO" required
        field.required("parent", Required.NO);

        // Assert the structure and formatting of the Group object
        assertEquals(
            "!Group {\n" +
            "  field: {\n" +
            "    required: {\n" +
            "      parent: NO\n" +
            "    }\n" +
            "  }\n" +
            "}\n", g.toString()
        );
    }
}
