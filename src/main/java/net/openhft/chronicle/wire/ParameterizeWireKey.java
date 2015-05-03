package net.openhft.chronicle.wire;

import net.openhft.chronicle.wire.WireKey;

/**
 * Created by Rob Austin
 */
public interface ParameterizeWireKey extends WireKey {
    <P extends WireKey> P[] params();
}
