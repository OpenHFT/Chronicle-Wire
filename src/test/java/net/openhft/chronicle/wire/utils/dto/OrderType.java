/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
 */

package net.openhft.chronicle.wire.utils.dto;

/**
 * Enum representing different types of orders in a trading context.
 */
public enum OrderType {
    /**
     * Represents a market order type.
     * A market order is executed immediately at the current market price.
     */
    market,

    /**
     * Represents a limit order type.
     * A limit order sets the maximum or minimum price at which you are willing to buy or sell.
     */
    limit
}
