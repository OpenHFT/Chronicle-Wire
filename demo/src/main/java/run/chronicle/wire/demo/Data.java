package run.chronicle.wire.demo;

import net.openhft.chronicle.bytes.util.BinaryLengthLength;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;

import java.util.concurrent.TimeUnit;

/**
 * This is the code for the data type Data, used in the Examples.
 */
class Data extends SelfDescribingMarshallable {
    private String message;
    private long number;
    private TimeUnit timeUnit;
    private double price;

    public Data() {
    }

    public Data(String message, long number, TimeUnit timeUnit, double price) {
        this.message = message;
        this.number = number;
        this.timeUnit = timeUnit;
        this.price = price;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getNumber() {
        return number;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    @Override
    public void readMarshallable(WireIn wire) throws IllegalStateException {
        wire.read("message").text(this, Data::setMessage)
                .read("number").int64(this, Data::setNumber)
                .read("timeUnit").asEnum(TimeUnit.class, this, Data::setTimeUnit)
                .read("price").float64(this, Data::setPrice);
    }

    @Override
    public void writeMarshallable(WireOut wire) {
        wire.write("message").text(message)
                .write("number").int64(number)
                .write("timeUnit").asEnum(timeUnit)
                .write("price").float64(price);
    }

    @Override
    public String toString() {
        return "Data{" +
                "message='" + message + '\'' +
                ", number=" + number +
                ", timeUnit=" + timeUnit +
                ", price=" + price +
                '}';
    }

    @Override
    public BinaryLengthLength binaryLengthLength() {
        return BinaryLengthLength.LENGTH_8BIT;
    }
}