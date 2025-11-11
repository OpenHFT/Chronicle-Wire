/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

/**
 * Represents funding information in a financial context.
 * This class is designed to be marshalled and unmarshalled easily with Chronicle Wire.
 */
public class Funding extends SelfDescribingMarshallable {

    private long symbol;          // Symbol identifier for the funding
    private double fr = Double.NaN; // Funding rate, defaulting to NaN (not a number)
    private long mins;            // Minutes until funding

    /**
     * Sets the funding rate. Infinite values are converted to NaN.
     *
     * @param fundingRate The funding rate to set.
     * @return This Funding instance for method chaining.
     */
    public Funding fr(final double fundingRate) {
        this.fr = Double.isInfinite(fundingRate) ? Double.NaN : fundingRate;
        return this;
    }

    /**
     * Gets the funding rate.
     *
     * @return The funding rate.
     */
    public double fr() {
        return fr;
    }

    /**
     * Gets the symbol.
     *
     * @return The symbol identifier.
     */
    public long s() {
        return symbol;
    }

    /**
     * Sets the symbol.
     *
     * @param symbol The symbol identifier to set.
     * @return This Funding instance for method chaining.
     */
    public Funding s(final long symbol) {
        this.symbol = symbol;
        return this;
    }

    /**
     * Gets the minutes until funding.
     *
     * @return The minutes until funding.
     */
    public long mins() {
        return mins;
    }

    /**
     * Sets the minutes until funding.
     *
     * @param minsUntilFunding The number of minutes until funding.
     */
    public void mins(long minsUntilFunding) {
        this.mins = minsUntilFunding;
    }
}
