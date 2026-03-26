/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValueOutMapTest extends WireTestCommon {

    @Test
    void writesAndReadsMaps() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "alice");
        map.put("count", 12L);

        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.BINARY.apply(bytes);
        wire.write("map").map(map);

        bytes.readPositionRemaining(0, bytes.writePosition());
        Map<String, Object> read = wire.read("map").marshallableAsMap(String.class, Object.class);
        assertEquals(map, read);
    }
}

