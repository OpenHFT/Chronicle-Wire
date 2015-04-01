package net.openhft.chronicle.wire;

import net.openhft.chronicle.util.CharSequenceComparator;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public class VanillaWireParser implements WireParser {
    final Map<CharSequence, Consumer<ValueIn>> namedConsumer = new TreeMap<>(CharSequenceComparator.INSTANCE);
    final Map<Integer, Consumer<ValueIn>> numberedConsumer = new HashMap<>();

    @Override
    public void register(WireKey key, Consumer<ValueIn> valueInConsumer) {
        namedConsumer.put(key.name(), valueInConsumer);
        numberedConsumer.put(key.code(), valueInConsumer);
    }

    @Override
    public Consumer<ValueIn> lookup(CharSequence name) {
        return namedConsumer.get(name);
    }

    @Override
    public Consumer<ValueIn> lookup(int number) {
        return numberedConsumer.get(number);
    }
}
