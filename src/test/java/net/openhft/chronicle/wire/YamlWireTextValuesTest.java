/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;

public class YamlWireTextValuesTest extends WireAsTextTestCommon {

    @Override
    protected Wire wireWithText(String... values) {
        YamlWire wire = new YamlWire(Bytes.allocateElasticOnHeap());
        for (String value : values)
            wire.write().text(value);
        return wire;
    }
}
