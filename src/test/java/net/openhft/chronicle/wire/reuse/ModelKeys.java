package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.wire.WireKey;

/**
 * Created by peter_2 on 13/02/2016.
 */
enum ModelKeys implements WireKey {
    id, revision, properties, collections, reference, path, name, value, key;

    @Override
    public int code() {
        return ordinal();
    }
}
