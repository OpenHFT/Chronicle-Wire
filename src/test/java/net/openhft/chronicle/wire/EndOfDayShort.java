/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

// Model class representing end of day short information for a stock.
class EndOfDayShort extends SelfDescribingMarshallable implements Serializable {
    private static final long serialVersionUID = 0L;
    // Symbol,Company,Price,Change,ChangePercent,Day's Volume
    private String name;
    private double closingPrice;
    private double change;
    private double changePercent;
    private long daysVolume;

    // Define how the object is serialized into wire format.
    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        wire.write(() -> "name").text(name)
                .write(() -> "price").float64(closingPrice)
                .write(() -> "change").float64(change)
                .write(() -> "changePercent").float64(changePercent)
                .write(() -> "daysVolume").int64(daysVolume);
    }
}
