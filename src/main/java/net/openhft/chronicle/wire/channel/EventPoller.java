package net.openhft.chronicle.wire.channel;

import net.openhft.chronicle.core.io.Closeable;

public interface EventPoller extends Closeable {
    boolean onPoll(ChronicleChannel channel);
}
