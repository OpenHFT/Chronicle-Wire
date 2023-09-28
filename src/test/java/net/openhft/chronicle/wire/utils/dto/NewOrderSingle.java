/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
 */

package net.openhft.chronicle.wire.utils.dto;

import net.openhft.chronicle.wire.converter.Base85;
import net.openhft.chronicle.wire.converter.NanoTime;

public class NewOrderSingle extends AbstractEvent<NewOrderSingle> {
    private static final int MASHALLABLE_VERSION = 1;
    @Base85
    private long symbol;
    @NanoTime
    private long transactTime;
    private double orderQty;
    private double price;
    private BuySell side;
    private String clOrdID = "";
    private OrderType ordType;

    public String clOrdID() {
        return clOrdID;
    }

    public NewOrderSingle clOrdID(String clOrdID) {
        this.clOrdID = clOrdID;
        return this;
    }

    public long symbol() {
        return symbol;
    }

    public NewOrderSingle symbol(long symbol) {
        this.symbol = symbol;
        return this;
    }

    public BuySell side() {
        return side;
    }

    public NewOrderSingle side(BuySell side) {
        this.side = side;
        return this;
    }

    public long transactTime() {
        return transactTime;
    }

    public NewOrderSingle transactTime(long transactTime) {
        this.transactTime = transactTime;
        return this;
    }

    public double orderQty() {
        return orderQty;
    }

    public NewOrderSingle orderQty(double orderQty) {
        this.orderQty = orderQty;
        return this;
    }

    public double price() {
        return price;
    }

    public NewOrderSingle price(double price) {
        this.price = price;
        return this;
    }

    public OrderType ordType() {
        return ordType;
    }

    public NewOrderSingle ordType(OrderType ordType) {
        this.ordType = ordType;
        return this;
    }
}
