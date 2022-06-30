package net.openhft.chronicle.wire.method;

public interface EventHandler<T extends Event> {
    void event(T event);

    void onEvent(T event);
}
