package net.openhft.chronicle.wire;

import net.openhft.chronicle.wire.WireKey;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Rob Austin
 */
public interface ParameterizeWireKey extends WireKey {
    @NotNull
    <P extends WireKey> P[] params();
}
