package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.AbstractMarshallable;
import net.openhft.chronicle.wire.WireOut;
import net.openhft.chronicle.wire.Wires;
import org.jetbrains.annotations.NotNull;

/*
 * Created by Peter Lawrey on 16/05/2017.
 */
class Rung extends AbstractMarshallable {
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
