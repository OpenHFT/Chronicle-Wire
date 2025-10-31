/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Verifies behaviour when fields are absent vs explicitly null.
 */
public class DefaultValueInEdgeCasesTest extends WireTestCommon {

    public static class WithDefaults extends SelfDescribingMarshallable {
        int i = 7;
        String s = "d";
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
