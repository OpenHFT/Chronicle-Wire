package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireOut;
import net.openhft.chronicle.wire.Wires;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("rawtypes")
public class WithDefaults extends SelfDescribingMarshallable {
    Bytes<?> bytes = Bytes.from("Hello");
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
