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

import net.openhft.chronicle.wire.Base85LongConverter;
import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;

import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

public final class MarketData extends SelfDescribingMarshallable {

    @LongConversion(Base85LongConverter.class)
    private long symbol;
    private double last;
    private double high;
    private double low;

    public MarketData() {
    }

    public MarketData(long symbol, double last, double high, double low) {
        symbol(symbol);
        last(last);
        high(high);
        low(low);
    }

    public MarketData(String symbol, double last, double high, double low) {
        symbol(requireNonNull(symbol));
        last(last);
        high(high);
        low(low);
    }

    public MarketData(MarketData other) {
        symbol(other.symbol);
        last(other.last);
        high(other.high);
        low(other.low);
    }

    public String symbol() {
        return Base85LongConverter.INSTANCE.asString(symbol);
    }

    public long symbolAsLong() {
        return symbol;
    }

    public void symbol(long symbol) {
        this.symbol = symbol;
    }

    public void symbol(String symbol) {
        this.symbol = Base85LongConverter.INSTANCE.parse(symbol);
    }

    public double last() {
        return last;
    }

    public void last(double last) {
        this.last = last;
    }

    public double high() {
        return high;
    }

    public void high(double high) {
        this.high = high;
    }

    public double low() {
        return low;
    }

    public void low(double low) {
        this.low = low;
    }

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
}