package net.openhft.chronicle.wire;

/**
 * Created by peter.lawrey on 31/01/2016.
 */
public interface ValueWriter<T> {
    void writeValue(T t, ValueOut valueOut);
}
