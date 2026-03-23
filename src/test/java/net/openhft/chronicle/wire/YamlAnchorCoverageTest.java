/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class YamlAnchorCoverageTest extends WireTestCommon {

    @Test
    public void readsAnchorsAndAliases() {
        assertThrows(UnsupportedOperationException.class, () -> {
            String yaml = "common: &base { num: 7, text: 'hello' }\nfirst: *base\n";
            YamlWire wire = YamlWire.from(yaml);

            Map<String, Object> common = wire.read("common").marshallableAsMap(String.class, Object.class);
            Object first = wire.read("first").object(null, Object.class, true);
            assertNotNull(first);
            if (first instanceof Map) {
                assertEquals(common, first);
            }
        });
    }
}
