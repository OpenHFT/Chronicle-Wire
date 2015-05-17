package net.openhft.chronicle.wire.map;

import net.openhft.chronicle.wire.ValueIn;
import net.openhft.chronicle.wire.ValueOut;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Created by Rob Austin
 */
public interface MapHandlerFunction {
    <V> BiConsumer<ValueOut, V> getKeyToWire();

    <V> Function<ValueIn, V> getWireToKey();

    <V> BiConsumer<ValueOut, V> getValueToWire();

    <V> Function<ValueIn, V> getWireToValue();

    <V> BiConsumer<ValueOut, Map.Entry<V, V>> getEntryToWire();

    <V> Function<ValueIn, Map.Entry<V, V>> getWireToEntry();

    <V> V usingValue();
}
