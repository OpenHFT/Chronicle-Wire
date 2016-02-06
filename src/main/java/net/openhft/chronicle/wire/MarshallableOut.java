package net.openhft.chronicle.wire;

import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * Created by peter.lawrey on 06/02/2016.
 */
public interface MarshallableOut {
    void send(WireKey key, WriteValue value);

    default void marshallable(WriteMarshallable object) {
        marshallable(object, ValueOut::marshallable);
    }

    <T> void marshallable(T t, BiConsumer<ValueOut, T> writer);

    default void typedMarshallable(@Nullable WriteMarshallable object) {
        marshallable(object, ValueOut::marshallable);
    }

    default <T> void typedMarshallable(T t, BiConsumer<ValueOut, T> writer) {
        marshallable(t, writer);
    }
}
