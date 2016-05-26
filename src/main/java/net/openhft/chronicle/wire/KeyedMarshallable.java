package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;

/**
 * @author Rob Austin.
 */
public interface KeyedMarshallable {
    default void writeKey(@NotNull Bytes bytes) {
        Wires.writeKey(this, bytes);
    }
}
