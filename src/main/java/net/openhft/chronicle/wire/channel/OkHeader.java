package net.openhft.chronicle.wire.channel;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

public class OkHeader extends SelfDescribingMarshallable implements ChannelHeader {
    private SystemContext systemContext;

    @Override
    public OkHeader systemContext(SystemContext systemContext) {
        this.systemContext = systemContext;
        return this;
    }

    @Override
    public SystemContext systemContext() {
        return systemContext;
    }

}
