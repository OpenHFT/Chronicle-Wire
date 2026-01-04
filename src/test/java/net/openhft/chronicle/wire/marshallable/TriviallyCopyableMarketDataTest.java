/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.Marshallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TriviallyCopyableMarketDataTest extends net.openhft.chronicle.wire.WireTestCommon {
    @Test
    @DisplayName("Trivially copyable market data round-trips")
    void test() {
        ClassAliasPool.CLASS_ALIASES.addAlias(TriviallyCopyableMarketData.class, "MarketData");
        final String str = "!MarketData {\n" +
                "  securityId: EUR/GBP,\n" +
                "  time: 2021-12-07T17:00:47.626128,\n" +
                "  bid0: 0.84963,\n" +
                "  bid1: 0.84964,\n" +
                "  bid2: 0.84965,\n" +
                "  bid3: 0.84967,\n" +
                "  bidQty0: 1E6,\n" +
                "  bidQty1: 2E6,\n" +
                "  bidQty2: 3E6,\n" +
                "  bidQty3: 4E6,\n" +
                "  ask0: 0.84961,\n" +
                "  ask1: 0.8496,\n" +
                "  ask2: 0.84959,\n" +
                "  ask3: 0.84958,\n" +
                "  askQty0: 1E6,\n" +
                "  askQty1: 3E6,\n" +
                "  askQty2: 5E6,\n" +
                "  askQty3: 2E6\n" +
                "}\n";
        TriviallyCopyableMarketData data = Marshallable.fromString(str);

        Bytes<?> bytes = Bytes.allocateElasticDirect();
        data.writeMarshallable(bytes);

        TriviallyCopyableMarketData data2 = new TriviallyCopyableMarketData();
        data2.readMarshallable(bytes);

        assertEquals(str, data.toString(), "Source object should render to the expected text");
        assertEquals(str, data2.toString(), "Round-tripped object should render to the expected text");
        assertEquals(data, data2, "Round-tripped object should match the source");
        assertEquals(data.fieldChecksum(), data2.fieldChecksum(),
                "Round-tripped object should preserve all field values");
    }
}
