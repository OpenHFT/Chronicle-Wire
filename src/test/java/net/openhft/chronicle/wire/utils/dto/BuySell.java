/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
 */

package net.openhft.chronicle.wire.utils.dto;

/**
 * Enum representing the possible buy/sell actions in a trading context.
 * Each enum constant is associated with a direction indicator.
 */
public enum BuySell {
    /**
     * Represents a buy action. Associated with a positive direction value.
     */
    buy(+1),

    /**
     * Represents a sell action. Associated with a negative direction value.
     */
    sell(-1);

    /**
     * The direction indicator for this buy/sell action.
     * Positive for buy, negative for sell.
     */
    public final int direction;

    /**
     * Constructs a BuySell enum constant with the specified direction.
     *
     * @param direction The direction indicator for this buy/sell action.
     */
    BuySell(int direction) {
        this.direction = direction;
    }
}
