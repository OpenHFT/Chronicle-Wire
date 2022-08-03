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

public final class MinMax {

    private double min = Double.MAX_VALUE;
    private double max = Double.MIN_VALUE;

    public MinMax() {
    }

    public MinMax(MarketData marketData) {
        this();
        merge(marketData);
    }

    public synchronized double min() {
        return min;
    }

    public synchronized double max() {
        return max;
    }

    synchronized MinMax merge(final MinMax other) {
        min = Math.min(this.min, other.min);
        max = Math.max(this.max, other.max);
        return this;
    }

    synchronized MinMax merge(final MarketData marketData) {
        min = Math.min(this.min, marketData.last());
        max = Math.max(this.max, marketData.last());
        return this;
    }

    @Override
    public String toString() {
        return "MinMax{" +
                "min=" + min +
                ", max=" + max +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MinMax minMax = (MinMax) o;

        if (Double.compare(minMax.min, min) != 0) return false;
        return Double.compare(minMax.max, max) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(min);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(max);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}
