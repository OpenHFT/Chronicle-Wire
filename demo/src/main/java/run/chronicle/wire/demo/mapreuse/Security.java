/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package run.chronicle.wire.demo.mapreuse;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

/**
 * DTO representing a financial security used in the map reuse example.
 */
public final class Security extends SelfDescribingMarshallable {

    /** security identifier */
    private int id;
    /** averaged price for the example */
    private long averagePrice;
    /** trade count */
    private long count;

    public Security(int id, long price, long count) {
        this.id = id;
        this.averagePrice = price;
        this.count = count;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(long averagePrice) {
        this.averagePrice = averagePrice;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "Security{" +
                "id=" + id +
                ", averagePrice=" + averagePrice +
                ", count=" + count +
                '}';
    }
}
