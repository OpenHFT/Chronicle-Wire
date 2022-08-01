package net.openhft.chronicle.wire.channel.internal;

import net.openhft.chronicle.bytes.MethodId;

public interface EventListener {
    @MethodId(1)
    void event(ChronicleEvent event);
}
