package net.openhft.chronicle.wire.channel;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RedirectHeader extends SelfDescribingMarshallable implements ChannelHeader {
    private final List<String> locations = new ArrayList<>();
    private SystemContext systemContext;

    public RedirectHeader(List<String> locations) {
        this.locations.addAll(locations);
    }

    @Override
    public RedirectHeader systemContext(SystemContext systemContext) {
        this.systemContext = systemContext;
        return this;
    }

    @Override
    public SystemContext systemContext() {
        return systemContext;
    }

    public List<String> locations() {
        return Collections.unmodifiableList(locations);
    }
}
