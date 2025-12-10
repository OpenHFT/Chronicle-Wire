/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.domestic.streaming.reduction;

import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.domestic.reduction.Reduction;
import net.openhft.chronicle.wire.domestic.streaming.CreateUtil;

import java.util.List;

@SuppressWarnings("deprecation")
final class MarketDataReductionTestSupport {
    private MarketDataReductionTestSupport() {
    }

    static void acceptData(Reduction<?> listener, List<MarketData> marketData) {
        Wire wire = CreateUtil.create();
        marketData.forEach(md -> write(wire, md));
        listener.accept(wire);
    }

    private static void write(Wire appender, MarketData marketData) {
        try (final DocumentContext dc = appender.writingDocument()) {
            dc.wire().getValueOut().object(marketData);
        }
    }
}
