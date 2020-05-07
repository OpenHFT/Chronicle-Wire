package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireOut;
import net.openhft.chronicle.wire.Wires;
import org.jetbrains.annotations.NotNull;


class Rung extends SelfDescribingMarshallable {
    double price, qty;
    boolean delta;
    String notSet;

    public static void main(String[] args) {
        Rung x = new Rung();
        x.price = 1.234;
        x.qty = 1e6;
        System.out.println(x);
    }

    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        Wires.writeMarshallable(this, wire, false);
    }
}
