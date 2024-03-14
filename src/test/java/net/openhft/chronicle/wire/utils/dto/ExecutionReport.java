/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
 */

package net.openhft.chronicle.wire.utils.dto;

import net.openhft.chronicle.wire.converter.NanoTime;
import net.openhft.chronicle.wire.converter.ShortText;

/**
 * Class representing an execution report for a financial order.
 * It extends {@link AbstractEvent} to inherit common event properties.
 */
public class ExecutionReport extends AbstractEvent<ExecutionReport> {
    /**
     * Version identifier for marshalling purposes.
     */
    private static final int MASHALLABLE_VERSION = 1;
    @ShortText
    private long symbol;

    /**
     * Timestamp of the transaction.
     */
    @NanoTime
    private long transactTime;

    /**
     * Quantity of the order.
     */
    private double orderQty;

    /**
     * Price per unit for the order.
     */
    private double price;

    /**
     * Unique identifier for the order.
     */
    @NanoTime
    private long orderID;

    /**
     * Last traded price of the order.
     */
    private double lastPx;

    /**
     * Remaining quantity of the order.
     */
    private double leavesQty;

    /**
     * Accumulated executed quantity of the order.
     */
    private double cumQty;

    /**
     * Average price of the order.
     */
    private double avgPx;

    /**
     * Buy or sell side of the order.
     */
    private BuySell side;

    /**
     * Order type (e.g., market, limit).
     */
    private OrderType ordType;

    /**
     * A client-assigned unique identifier for the order.
     */
    private String clOrdID = "";

    /**
     * Additional text information about the order or its execution.
     */
    private String text = null;

    // Getter and setter methods for each field
    public String clOrdID() {
        return clOrdID;
    }

    public ExecutionReport clOrdID(String clOrdID) {
        this.clOrdID = clOrdID;
        return this;
    }

    public long symbol() {
        return symbol;
    }

    public ExecutionReport symbol(long symbol) {
        this.symbol = symbol;
        return this;
    }

    public BuySell side() {
        return side;
    }

    public ExecutionReport side(BuySell side) {
        this.side = side;
        return this;
    }

    public long transactTime() {
        return transactTime;
    }

    public ExecutionReport transactTime(long transactTime) {
        this.transactTime = transactTime;
        return this;
    }

    public double orderQty() {
        return orderQty;
    }

    public ExecutionReport orderQty(double orderQty) {
        this.orderQty = orderQty;
        return this;
    }

    /**
     * Retrieves the price per unit of the order.
     *
     * @return Price per unit as a double.
     */
    public double price() {
        return price;
    }

    /**
     * Sets the price per unit for the order and returns the updated {@link ExecutionReport} instance.
     *
     * @param price Price per unit as a double.
     * @return Updated {@link ExecutionReport} instance.
     */
    public ExecutionReport price(double price) {
        this.price = price;
        return this;
    }

    /**
     * Retrieves the unique identifier for the order.
     *
     * @return Order ID as a long.
     */
    public long orderID() {
        return orderID;
    }

    /**
     * Sets the order ID and returns the updated {@link ExecutionReport} instance.
     *
     * @param orderID Unique identifier for the order.
     * @return Updated {@link ExecutionReport} instance.
     */
    public ExecutionReport orderID(long orderID) {
        this.orderID = orderID;
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
     * Sets the order type and returns the updated {@link ExecutionReport} instance.
     *
     * @param ordType Type of the order.
     * @return Updated {@link ExecutionReport} instance.
     */
    public ExecutionReport ordType(OrderType ordType) {
        this.ordType = ordType;
        return this;
    }

    /**
     * Retrieves the last traded price of the order.
     *
     * @return Last traded price as a double.
     */
    public double lastPx() {
        return lastPx;
    }

    /**
     * Sets the last traded price for the order and returns the updated {@link ExecutionReport} instance.
     *
     * @param lastPx Last traded price.
     * @return Updated {@link ExecutionReport} instance.
     */
    public ExecutionReport lastPx(double lastPx) {
        this.lastPx = lastPx;
        return this;
    }

    /**
     * Retrieves the remaining quantity of the order.
     *
     * @return Remaining quantity as a double.
     */
    public double leavesQty() {
        return leavesQty;
    }

    /**
     * Sets the remaining quantity of the order and returns the updated {@link ExecutionReport} instance.
     *
     * @param leavesQty Remaining quantity of the order.
     * @return Updated {@link ExecutionReport} instance.
     */
    public ExecutionReport leavesQty(double leavesQty) {
        this.leavesQty = leavesQty;
        return this;
    }

    /**
     * Retrieves the accumulated executed quantity of the order.
     *
     * @return Accumulated quantity as a double.
     */
    public double cumQty() {
        return cumQty;
    }

    /**
     * Sets the accumulated executed quantity for the order and returns the updated {@link ExecutionReport} instance.
     *
     * @param cumQty Accumulated executed quantity.
     * @return Updated {@link ExecutionReport} instance.
     */
    public ExecutionReport cumQty(double cumQty) {
        this.cumQty = cumQty;
        return this;
    }

    /**
     * Retrieves the average price of the order.
     *
     * @return Average price as a double.
     */
    public double avgPx() {
        return avgPx;
    }

    /**
     * Sets the average price for the order and returns the updated {@link ExecutionReport} instance.
     *
     * @param avgPx Average price of the order.
     * @return Updated {@link ExecutionReport} instance.
     */
    public ExecutionReport avgPx(double avgPx) {
        this.avgPx = avgPx;
        return this;
    }

    /**
     * Retrieves the additional text information about the order or its execution.
     *
     * @return Text information as a string.
     */
    public String text() {
        return text;
    }

    /**
     * Sets the additional text information for the order and returns the updated {@link ExecutionReport} instance.
     *
     * @param text Additional text information.
     * @return Updated {@link ExecutionReport} instance.
     */
    public ExecutionReport text(String text) {
        this.text = text;
        return this;
    }
}
