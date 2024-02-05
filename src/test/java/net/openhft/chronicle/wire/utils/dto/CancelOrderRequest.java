/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
 */

package net.openhft.chronicle.wire.utils.dto;

import net.openhft.chronicle.wire.converter.ShortText;

public class CancelOrderRequest extends AbstractEvent<CancelOrderRequest> {
    private static final int MASHALLABLE_VERSION = 1;
    @ShortText
    private long symbol;
    private String clOrdID = "";

    public String clOrdID() {
        return clOrdID;
    }

    public CancelOrderRequest clOrdID(String clOrdID) {
        this.clOrdID = clOrdID;
        return this;
    }

    public long symbol() {
        return symbol;
    }

    public CancelOrderRequest symbol(long symbol) {
        this.symbol = symbol;
        return this;
    }
}
