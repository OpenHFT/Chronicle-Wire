package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireOut;
import net.openhft.chronicle.wire.Wires;
import org.jetbrains.annotations.NotNull;

/*
 * Created by Peter Lawrey on 26/05/2017.
 */
@SuppressWarnings("rawtypes")
public class WithDefaults extends SelfDescribingMarshallable {
    Bytes bytes = Bytes.fromString("Hello");
    String text = "Hello";
    boolean flag = true;
    int num = Integer.MIN_VALUE;
    Long num2 = Long.MIN_VALUE;
    double qty = Double.NaN;

    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        Wires.writeMarshallable(this, wire, false);
    }
}
