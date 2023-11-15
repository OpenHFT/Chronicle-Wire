/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
 */

package net.openhft.chronicle.wire.utils.dto;

import net.openhft.chronicle.wire.converter.ShortText;

/**
 * Represents an order cancellation rejection with various cancellation details.
 */
public class OrderCancelReject extends AbstractEvent<OrderCancelReject> {
    private static final int MASHALLABLE_VERSION = 1;
    @ShortText
    private long symbol;
    private String clOrdID = "";
    private String reason = "";

    private String clOrdID = ""; // Client order ID.
    private String reason = ""; // Reason for the cancellation rejection.

    /**
     * Retrieves the client order ID associated with the cancellation rejection.
     *
     * @return The client order ID as a String.
     */
    public String clOrdID() {
        return clOrdID;
    }

    /**
     * Sets the client order ID for this cancellation rejection.
     *
     * @param clOrdID The client order ID as a String.
     * @return The current instance of OrderCancelReject.
     */
    public OrderCancelReject clOrdID(String clOrdID) {
        this.clOrdID = clOrdID;
        return this;
    }

    /**
     * Retrieves the symbol of the order associated with the cancellation rejection.
     *
     * @return The symbol as a long value.
     */
    public long symbol() {
        return symbol;
    }

    /**
     * Sets the symbol of the order for this cancellation rejection.
     *
     * @param symbol The symbol of the order.
     * @return The current instance of OrderCancelReject.
     */
    public OrderCancelReject symbol(long symbol) {
        this.symbol = symbol;
        return this;
    }

    /**
     * Retrieves the reason for the cancellation rejection.
     *
     * @return The reason as a String.
     */
    public String reason() {
        return reason;
    }

    /**
     * Sets the reason for this cancellation rejection.
     *
     * @param reason The reason for the rejection.
     * @return The current instance of OrderCancelReject.
     */
    public OrderCancelReject reason(String reason) {
        this.reason = reason;
        return this;
    }
}
