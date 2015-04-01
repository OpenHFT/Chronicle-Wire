package net.openhft.chronicle.wire;

import java.util.function.Consumer;

public interface WireParser {
    static WireParser wireParser() {
        return new VanillaWireParser();
    }

    void register(WireKey key, Consumer<ValueIn> valueInConsumer);

    Consumer<ValueIn> lookup(CharSequence name);

    Consumer<ValueIn> lookup(int number);
}
