package net.openhft.chronicle.wire;

/**
 * Created by peter.lawrey on 31/01/2016.
 */
public interface MarshallableParser<T> {
    T parse(ValueIn valueIn);
}
