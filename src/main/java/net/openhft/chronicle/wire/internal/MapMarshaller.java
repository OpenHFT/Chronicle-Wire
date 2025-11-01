/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.wire.ValueOut;
import net.openhft.chronicle.wire.WireOut;
import net.openhft.chronicle.wire.WriteMarshallable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * MapMarshaller is a utility for serializing a Map into a Wire format.
 * This is an inner class used for handling the custom marshalling process for Map objects.
 * Its primary function is to loop through a Map's entries and write each key-value pair to the Wire.
 */
public class MapMarshaller<K, V> implements WriteMarshallable {
    private Map<K, V> map;
    private Class<K> kClass;
    private Class<V> vClass;
    private boolean leaf;

    /**
     * Configures the MapMarshaller with the provided parameters.
     *
     * @param map    The map to be marshalled.
     * @param kClass The class type of the map's key.
     * @param vClass The class type of the map's value.
     * @param leaf   A flag indicating if the current node is a leaf in a structure.
     */
    public void params(@Nullable Map<K, V> map, @NotNull Class<K> kClass, @NotNull Class<V> vClass, boolean leaf) {
        this.map = map;
        this.kClass = kClass;
        this.vClass = vClass;
        this.leaf = leaf;
    }

    /**
     * Converts and writes the Map's entries to the Wire format.
     *
     * @param wire The WireOut instance to write to.
     */
    @Override
    public void writeMarshallable(@NotNull WireOut wire) throws InvalidMarshallableException {
        for (@NotNull Map.Entry<K, V> entry : map.entrySet()) {
            ValueOut valueOut = wire.writeEvent(kClass, entry.getKey());
            boolean wasLeaf = valueOut.swapLeaf(leaf);
            valueOut.object(vClass, entry.getValue());
            valueOut.swapLeaf(wasLeaf);
        }
    }
}
