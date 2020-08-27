package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

public class Funding extends SelfDescribingMarshallable {

    private long symbol;

    private double fr = Double.NaN;

    private long mins;

    public Funding fr(final double fundingRate) {
        this.fr = Double.isInfinite(fundingRate) ? Double.NaN : fundingRate;
        return this;
    }

    public double fr() {
        return fr;
    }

    public long s() {
        return symbol;
    }

    public Funding s(final long symbol) {
        this.symbol = symbol;
        return this;
    }

    public long mins() {
        return mins;
    }

    public void mins(long minsUntilFunding) {
        this.mins = minsUntilFunding;
    }

}
