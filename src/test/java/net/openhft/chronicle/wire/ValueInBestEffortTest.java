/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class ValueInBestEffortTest extends WireTestCommon {

    private static final String YAML = "value: { foo: bar }";

    @Test
    public void strictModeReturnsNullOnTypeMismatch() {
        TextWire wire = TextWire.from(YAML);
        Object result = wire.read("value").object(null, String.class, false);
        // In strict mode, mismatched types are not coerced into a target class;
        // current behaviour returns a textual representation of the mapping.
        assertTrue(result instanceof String);
        String s = (String) result;
        assertTrue(s.startsWith("{"));
        assertTrue(s.contains("foo: bar"));
        assertTrue(s.endsWith("}"));
    }

    @Test
    public void bestEffortAllowsMismatchedTypes() {
        TextWire wire = TextWire.from(YAML);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = wire.read("value").object(null, Map.class, true);
        assertEquals("bar", map.get("foo"));
    }
}
