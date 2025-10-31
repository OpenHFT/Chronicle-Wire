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

public class YamlValueOutAdditionalTest extends WireTestCommon {

    @Test
    public void writesQuotedAndMultilineValues() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        YamlWire wire = new YamlWire(bytes);

        wire.write("quoted").text("needs: quoting");
        wire.write("multiline").text("first\nsecond");
        wire.write("flag").bool(true);

        String yaml = bytes.toString();
        assertTrue(yaml.contains("quoted: \"needs: quoting\""));

        Map<String, Object> values = YamlWire.from(yaml)
                .read().marshallableAsMap(String.class, Object.class);
        assertEquals("needs: quoting", values.get("quoted"));
        assertEquals("first\nsecond", values.get("multiline"));
        assertEquals(true, values.get("flag"));
    }
}

