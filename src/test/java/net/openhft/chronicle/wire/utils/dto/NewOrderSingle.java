/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
 */

package net.openhft.chronicle.wire.utils.dto;

import net.openhft.chronicle.wire.converter.NanoTime;
import net.openhft.chronicle.wire.converter.ShortText;

/**
 * Represents a new order single request with various order details.
 */
public class NewOrderSingle extends AbstractEvent<NewOrderSingle> {
    private static final int MASHALLABLE_VERSION = 1;
    @ShortText
    private long symbol;
    @NanoTime
    private long transactTime; // Transaction time of the order, in nanoseconds.
    private double orderQty; // Quantity of the order.
    private double price; // Price per unit of the order.
    private BuySell side; // Side of the order (buy or sell).
    private String clOrdID = ""; // Client order ID.
    private OrderType ordType; // Type of the order (e.g., market, limit).

    /**
     * Retrieves the client order ID associated with this new order.
     *
     * @return The client order ID as a String.
     */
    public String clOrdID() {
        return clOrdID;
    }

    /**
     * Sets the client order ID and returns the updated {@link NewOrderSingle} instance.
     *
     * @param clOrdID Client order ID as a string.
     * @return Updated {@link NewOrderSingle} instance.
     */
    public NewOrderSingle clOrdID(String clOrdID) {
        this.clOrdID = clOrdID;
        return this;
    }

    /**
     * Retrieves the symbol associated with the order.
     *
     * @return Symbol as a long.
     */
    public long symbol() {
        return symbol;
    }

    /**
     * Sets the symbol for the order and returns the updated {@link NewOrderSingle} instance.
     *
     * @param symbol Symbol associated with the order.
     * @return Updated {@link NewOrderSingle} instance.
     */
    public NewOrderSingle symbol(long symbol) {
        this.symbol = symbol;
        return this;
    }

    /**
     * Retrieves the side of the order (buy or sell).
     *
     * @return Order side as a {@link BuySell} enum.
     */
    public BuySell side() {
        return side;
    }

    /**
     * Sets the side of the order (buy or sell) and returns the updated {@link NewOrderSingle} instance.
     *
     * @param side Order side (buy or sell).
     * @return Updated {@link NewOrderSingle} instance.
     */
    public NewOrderSingle side(BuySell side) {
        this.side = side;
        return this;
    }

    /**
     * Retrieves the transaction time for the order.
     *
     * @return Transaction time as a long.
     */
    public long transactTime() {
        return transactTime;
    }

    /**
     * Sets the transaction time for the order and returns the updated {@link NewOrderSingle} instance.
     *
     * @param transactTime Transaction time for the order.
     * @return Updated {@link NewOrderSingle} instance.
     */
    public NewOrderSingle transactTime(long transactTime) {
        this.transactTime = transactTime;
        return this;
    }

    /**
     * Retrieves the quantity of the order.
     *
     * @return Order quantity as a double.
     */
    public double orderQty() {
        return orderQty;
    }

    /**
     * Sets the quantity for the order and returns the updated {@link NewOrderSingle} instance.
     *
     * @param orderQty Order quantity.
     * @return Updated {@link NewOrderSingle} instance.
     */
    public NewOrderSingle orderQty(double orderQty) {
        this.orderQty = orderQty;
        return this;
    }

    /**
     * Retrieves the price per unit for the order.
     *
     * @return Price per unit as a double.
     */
    public double price() {
        return price;
    }

    /**
     * Sets the price per unit for the order and returns the updated {@link NewOrderSingle} instance.
     *
     * @param price Price per unit.
     * @return Updated {@link NewOrderSingle} instance.
     */
    public NewOrderSingle price(double price) {
        this.price = price;
        return this;
    }

    /**
     * Retrieves the order type.
     *
     * @return Order type as an {@link OrderType}.
     */
    public OrderType ordType() {
        return ordType;
    }

    /**
     * Sets the order type and returns the updated {@link NewOrderSingle} instance.
     *
     * @param ordType Type of the order.
     * @return Updated {@link NewOrderSingle} instance.
     */
    public NewOrderSingle ordType(OrderType ordType) {
        this.ordType = ordType;
        return this;
    }
}
