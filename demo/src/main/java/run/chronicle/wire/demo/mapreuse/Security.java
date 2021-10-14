package run.chronicle.wire.demo.mapreuse;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

public final class Security extends SelfDescribingMarshallable {

    private int id;
    private long averagePrice;
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
