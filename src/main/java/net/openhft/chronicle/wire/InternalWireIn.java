package net.openhft.chronicle.wire;

/**
 * These methods are for internal use only.
 *
 * Created by peter.lawrey on 17/04/15.
 */
public interface InternalWireIn extends WireIn {
    void setReady(boolean ready);
}
