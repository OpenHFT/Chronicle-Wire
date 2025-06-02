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

/**
 * Represents a range with a minimum and maximum value.
 * This class provides functionality to merge its state with other MinMax objects
 * or update its state based on a given MarketData object.
 */
public final class MinMax {

    // Holds the minimum value, initialized to the maximum possible value of a double.
    private double min = Double.MAX_VALUE;

    // Holds the maximum value, initialized to the smallest positive value of a double.
    private double max = Double.MIN_VALUE;

    // Default constructor.
    public MinMax() {
    }

    // Constructor that initializes the min/max values based on a given MarketData object.
    public MinMax(MarketData marketData) {
        this();
        merge(marketData);
    }

    // Getter for the minimum value.
    public synchronized double min() {
        return min;
    }

    // Getter for the maximum value.
    public synchronized double max() {
        return max;
    }

    // Merges the current object with another MinMax object and returns the updated current object.
    synchronized MinMax merge(final MinMax other) {
        min = Math.min(this.min, other.min);
        max = Math.max(this.max, other.max);
        return this;
    }

    // Merges the current object's values based on a MarketData object and returns the updated current object.
    synchronized MinMax merge(final MarketData marketData) {
        min = Math.min(this.min, marketData.last());
        max = Math.max(this.max, marketData.last());
        return this;
    }

    // Provides a string representation of the object.
    @Override
    public String toString() {
        return "MinMax{" +
                "min=" + min +
                ", max=" + max +
                '}';
    }

    // Overrides the equals method to compare MinMax objects based on their min and max values.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MinMax minMax = (MinMax) o;

        if (Double.compare(minMax.min, min) != 0) return false;
        return Double.compare(minMax.max, max) == 0;
    }

    // Overrides the hashCode method to provide a hash code based on the min and max values.
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
