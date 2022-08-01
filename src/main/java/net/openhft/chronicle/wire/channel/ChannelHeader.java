package net.openhft.chronicle.wire.channel;

import net.openhft.chronicle.wire.Marshallable;

public interface ChannelHeader extends Marshallable {
    ChannelHeader systemContext(SystemContext systemContext);

    SystemContext systemContext();
}