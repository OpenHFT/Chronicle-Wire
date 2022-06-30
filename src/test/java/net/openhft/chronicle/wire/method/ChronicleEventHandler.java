package net.openhft.chronicle.wire.method;

public interface ChronicleEventHandler extends EventHandler<ChronicleEvent> {
    @Override
    void event(ChronicleEvent event);
}
