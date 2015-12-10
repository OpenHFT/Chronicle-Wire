package net.openhft.chronicle.wire;

/**
 * Created by peter.lawrey on 10/10/2015.
 */
@FunctionalInterface
public interface ReadingMarshaller<T> {
    void readFromWire(T t, WireIn in);
}
