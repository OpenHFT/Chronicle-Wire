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

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Drives YamlValueOut formatting paths (strings needing quoting, multi-line, sequences, maps)
 * and validates via round-trip parsing.
 */
public class YamlValueOutFormattingBranchesTest extends WireTestCommon {

    @Test
    public void writeAndReadComplexYaml() {
        Wire w = WireType.YAML.apply(Bytes.allocateElasticOnHeap(512));

        // Values likely to exercise quoting and multi-line formatting branches
        w.write("quoted").text("needs: quoting [brackets]");
        w.write("multiline").text("line1\nline2");
        w.write("empty").text("");
        w.write("seq").sequence(v -> { v.text("x"); v.text("y"); });
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("k1", "v1");
        m.put("k2", 2L);
        w.write("map").map(m);

        String yaml = w.toString();

        // Parse back and assert values
        YamlWire r = YamlWire.from(yaml);
        assertEquals("needs: quoting [brackets]", r.read("quoted").text());
        assertEquals("line1\nline2", r.read("multiline").text());
        assertEquals("", r.read("empty").text());
        final Object[] seq = new Object[2];
        r.read("seq").sequence(seq, (arr, in) -> { arr[0] = in.text(); arr[1] = in.text(); });
        assertArrayEquals(new Object[]{"x", "y"}, seq);
        Map<?, ?> out = r.read("map").marshallableAsMap(String.class, Object.class);
        assertEquals(m, out);
    }
}

