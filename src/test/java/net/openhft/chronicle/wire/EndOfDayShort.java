/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

// Model class representing end of day short information for a stock.
public class EndOfDayShort extends SelfDescribingMarshallable implements Serializable {
    private static final long serialVersionUID = 0L;
    // Symbol,Company,Price,Change,ChangePercent,Day's Volume
    public String name;
    public double closingPrice, change, changePercent;
    long daysVolume;

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
