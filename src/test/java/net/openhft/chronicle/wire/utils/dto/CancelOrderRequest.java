/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
 */

package net.openhft.chronicle.wire.utils.dto;

import net.openhft.chronicle.wire.converter.ShortText;

/**
 * Class representing a request to cancel an order.
 * It extends {@link AbstractEvent} to inherit common event properties.
 */
public class CancelOrderRequest extends AbstractEvent<CancelOrderRequest> {
    /**
     * Version identifier for marshalling purposes.
     */
    private static final int MASHALLABLE_VERSION = 1;
    @ShortText
    private long symbol;

    /**
     * A client-assigned unique identifier for the order to be canceled.
     */
    private String clOrdID = "";

    /**
     * Retrieves the client order ID.
     *
     * @return Client order ID string.
     */
    public String clOrdID() {
        return clOrdID;
    }

    /**
     * Sets the client order ID for the cancellation request.
     *
     * @param clOrdID The client order ID.
     * @return The current instance of {@link CancelOrderRequest} for method chaining.
     */
    public CancelOrderRequest clOrdID(String clOrdID) {
        this.clOrdID = clOrdID;
        return this;
    }

    /**
     * Retrieves the symbol of the financial instrument.
     *
     * @return Symbol as a long value.
     */
    public long symbol() {
        return symbol;
    }

    /**
     * Sets the symbol of the financial instrument involved in the cancellation request.
     *
     * @param symbol The symbol as a long value.
     * @return The current instance of {@link CancelOrderRequest} for method chaining.
     */
    public CancelOrderRequest symbol(long symbol) {
        this.symbol = symbol;
        return this;
    }
}
