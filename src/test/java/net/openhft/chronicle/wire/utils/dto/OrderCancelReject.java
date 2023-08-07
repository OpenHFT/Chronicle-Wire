/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
 */

package net.openhft.chronicle.wire.utils.dto;

import net.openhft.chronicle.wire.converter.Base85;

public class OrderCancelReject extends AbstractEvent<OrderCancelReject> {
    private static final int MASHALLABLE_VERSION = 1;
    @Base85
    private long symbol;
    private String clOrdID = "";
    private String reason = "";

    public String clOrdID() {
        return clOrdID;
    }

    public OrderCancelReject clOrdID(String clOrdID) {
        this.clOrdID = clOrdID;
        return this;
    }

    public long symbol() {
        return symbol;
    }

    public OrderCancelReject symbol(long symbol) {
        this.symbol = symbol;
        return this;
    }

    public String reason() {
        return reason;
    }

    public OrderCancelReject reason(String reason) {
        this.reason = reason;
        return this;
    }
}
