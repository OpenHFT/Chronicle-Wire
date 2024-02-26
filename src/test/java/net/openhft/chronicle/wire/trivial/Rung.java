/*
 * Copyright 2019-2022 - http://chronicle.software
 *
 * Chronicle software holds the rights to this software and it may not be redistributed to another organisation or a different team within your organisation.
 *
 * You may only use this software if you have prior written consent from Chronicle Software.
 *
 * This written consent may take the form of a valid (non expired) software licence.
 */
package net.openhft.chronicle.wire.trivial;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

/**
 * Price/qty tuple, just a helper class for {@link Orderbook#toString()}.
 */
public class Rung extends SelfDescribingMarshallable {
    double price;
    double qty;

    public Rung(double price, double qty) {
        this.price = price;
        this.qty = qty;
    }

    public Rung() {
    }

    public double price() {
        return price;
    }

    public Rung price(double rate) {
        this.price = rate;
        return this;
    }

    public double qty() {
        return qty;
    }

    public Rung qty(long qty) {
        this.qty = qty;
        return this;
    }
}
