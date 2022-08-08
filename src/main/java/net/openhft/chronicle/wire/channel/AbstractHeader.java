/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under Contract only
 */

package net.openhft.chronicle.wire.channel;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

public class AbstractHeader<H extends AbstractHeader<H>>
        extends SelfDescribingMarshallable
        implements ChannelHeader {
    private SystemContext systemContext;
    private String sessionName;

    @Override
    public SystemContext systemContext() {
        return systemContext;
    }

    @Override
    public H systemContext(SystemContext systemContext) {
        this.systemContext = systemContext;
        return (H) this;
    }

    @Override
    public String sessionName() {
        return sessionName;
    }

    @Override
    public H sessionName(String sessionName) {
        this.sessionName = sessionName;
        return (H) this;
    }
}
