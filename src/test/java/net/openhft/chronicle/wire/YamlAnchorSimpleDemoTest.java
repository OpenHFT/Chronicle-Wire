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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Demonstrates YAML anchor functionality in Chronicle Wire
 */
public class YamlAnchorSimpleDemoTest extends WireTestCommon {

    public static class Config extends SelfDescribingMarshallable {
        public String text;
        public int value;
    }

    public static class MultiConfig extends SelfDescribingMarshallable {
        public Config first;
        public Config second;
        public Config third;
    }

    @Test
    public void shouldShareFieldValuesUsingAnchors() {
        String yaml = "" +
                "!net.openhft.chronicle.wire.YamlAnchorSimpleDemoTest$MultiConfig {\n" +
                "  first: {\n" +
                "    text: &sharedText hello,\n" +
                "    value: &sharedValue 42\n" +
                "  },\n" +
                "  second: {\n" +
                "    text: *sharedText,\n" +
                "    value: 100\n" +
                "  },\n" +
                "  third: {\n" +
                "    text: world,\n" +
                "    value: *sharedValue\n" +
                "  }\n" +
                "}\n";

        MultiConfig config = WireType.YAML.fromString(MultiConfig.class, yaml);

        // Show the results
        assertEquals("hello", config.first.text);
        assertSame(config.first.text, config.second.text);
        assertEquals("world", config.third.text);

        // Verify text anchors worked (numeric anchors seem to have issues)
        assertEquals(42, config.first.value);
        assertEquals(100, config.second.value);  // Same as first.text via anchor
        assertEquals(42, config.third.value);
    }

    @Test
    public void shouldShareObjectInstancesUsingAnchors() {
        String yaml = "" +
                "first: &shared !net.openhft.chronicle.wire.YamlAnchorSimpleDemoTest$Config {\n" +
                "  text: shared config,\n" +
                "  value: 999\n" +
                "}\n" +
                "second: *shared\n" +
                "third: !net.openhft.chronicle.wire.YamlAnchorSimpleDemoTest$Config {\n" +
                "  text: different config,\n" +
                "  value: 111\n" +
                "}\n";

        MultiConfig config = WireType.YAML.fromString(MultiConfig.class, yaml);

        // Show the results
        assertEquals("shared config", config.first.text);
        assertSame(config.first.text, config.second.text);
        assertEquals("different config", config.third.text);
    }
}
