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
