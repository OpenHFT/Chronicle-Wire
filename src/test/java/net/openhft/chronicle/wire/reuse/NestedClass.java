package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;

/**
 * Created by peter.lawrey on 01/02/2016.
 */
public class NestedClass implements Marshallable {
    String text;
    double number;

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        wire.read(() -> "text").text(this, (t, v) -> t.text = v)
                .read(() -> "number").float64(this, (t, v) -> t.number = v);
    }

    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        wire.write(() -> "text").text(text)
                .write(() -> "number").float64(number);
    }

    public void setTextNumber(String text, double number) {
        this.text = text;
        this.number = number;
    }

    @Override
    public String toString() {
        return "NestedClass{" +
                "text='" + text + '\'' +
                ", number=" + number +
                '}';
    }
}
