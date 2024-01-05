/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.domestic.streaming.reduction;

import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.Validatable;
import net.openhft.chronicle.core.io.ValidatableUtil;
import net.openhft.chronicle.wire.Base85LongConverter;
import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;

import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * Represents the market data for a particular stock symbol.
 * This class is self-describing, marshallable, and validatable.
 */
public final class MarketData extends SelfDescribingMarshallable implements Validatable {

    // Symbol for the market data, stored as a long but represented in base85 format for human readability
    @LongConversion(Base85LongConverter.class)
    private long symbol;

    // Last traded price of the stock
    private double last;

    // Highest traded price of the stock during a particular period
    private double high;

    // Lowest traded price of the stock during a particular period
    private double low;

    // Default constructor
    public MarketData() {
    }

    /**
     * Constructor using long symbol and pricing information.
     *
     * @param symbol Unique identifier for the stock.
     * @param last   Last traded price.
     * @param high   Highest traded price during the period.
     * @param low    Lowest traded price during the period.
     */
    public MarketData(long symbol, double last, double high, double low) {
        symbol(symbol);
        last(last);
        high(high);
        low(low);
    }

    /**
     * Constructor using string symbol and pricing information.
     *
     * @param symbol Unique identifier for the stock in string format.
     * @param last   Last traded price.
     * @param high   Highest traded price during the period.
     * @param low    Lowest traded price during the period.
     */
    public MarketData(String symbol, double last, double high, double low) {
        symbol(requireNonNull(symbol));
        last(last);
        high(high);
        low(low);
    }

    /**
     * Copy constructor to create a new instance based on another MarketData object.
     *
     * @param other The MarketData instance to copy data from.
     */
    public MarketData(MarketData other) {
        symbol(other.symbol);
        last(other.last);
        high(other.high);
        low(other.low);
    }

    /**
     * Retrieves the symbol in its string representation.
     *
     * @return Symbol represented as a string using Base85 encoding.
     */
    public String symbol() {
        return Base85LongConverter.INSTANCE.asString(symbol);
    }

    /**
     * Retrieves the symbol in its long representation.
     *
     * @return Symbol represented as a long.
     */
    public long symbolAsLong() {
        return symbol;
    }

    /**
     * Sets the symbol using its long representation.
     *
     * @param symbol Symbol represented as a long.
     */
    public void symbol(long symbol) {
        this.symbol = symbol;
    }

    /**
     * Sets the symbol using its string representation.
     *
     * @param symbol Symbol represented as a string.
     */
    public void symbol(String symbol) {
        this.symbol = Base85LongConverter.INSTANCE.parse(symbol);
    }

    /**
     * Retrieves the last traded price of the stock.
     *
     * @return Last traded price.
     */
    public double last() {
        return last;
    }

    /**
     * Sets the last traded price of the stock.
     *
     * @param last Last traded price.
     */
    public void last(double last) {
        this.last = last;
    }

    /**
     * Retrieves the highest traded price of the stock during a particular period.
     *
     * @return Highest traded price.
     */
    public double high() {
        return high;
    }

    /**
     * Sets the highest traded price of the stock during a particular period.
     *
     * @param high Highest traded price.
     */
    public void high(double high) {
        this.high = high;
    }

    /**
     * Retrieves the lowest traded price of the stock during a particular period.
     *
     * @return Lowest traded price.
     */
    public double low() {
        return low;
    }

    /**
     * Sets the lowest traded price of the stock during a particular period.
     *
     * @param low Lowest traded price.
     */
    public void low(double low) {
        this.low = low;
    }

    /**
     * Compares this object with another object for equality.
     *
     * @param o The object to compare with.
     * @return True if objects are equal, otherwise false.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        MarketData that = (MarketData) o;

        if (symbol != that.symbol) return false;
        if (Double.compare(that.last, last) != 0) return false;
        if (Double.compare(that.high, high) != 0) return false;
        return Double.compare(that.low, low) == 0;
    }

    /**
     * Generates a hash code for this object.
     *
     * @return The hash code.
     */
    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp;
        result = 31 * result + (int) (symbol ^ (symbol >>> 32));
        temp = Double.doubleToLongBits(last);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(high);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(low);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    /**
     * Validates the fields of this object to ensure they meet expected criteria.
     *
     * @throws InvalidMarshallableException If validation fails.
     */
    @Override
    public void validate() throws InvalidMarshallableException {
        ValidatableUtil.requireTrue(symbol > 0, "symbol must be set");
        ValidatableUtil.requireTrue(last >= 0, "last must be non-negative");
        ValidatableUtil.requireTrue(high > 0, "high must be positive");
        ValidatableUtil.requireTrue(low > 0, "low must be positive");
    }
}
