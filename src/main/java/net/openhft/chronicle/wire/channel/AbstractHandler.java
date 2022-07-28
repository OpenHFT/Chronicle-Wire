package net.openhft.chronicle.wire.channel;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

/**
 * Abstract class supporting the common fields implied by ChannelHandler
 *
 * @param <A> the same class so setters can return this
 */
public abstract class AbstractHandler<A extends AbstractHandler<A>> extends SelfDescribingMarshallable implements ChannelHandler {
    private SystemContext systemContext;
    private Boolean buffered;

    @Override
    public SystemContext systemContext() {
        return systemContext;
    }

    @Override
    public A systemContext(SystemContext systemContext) {
        this.systemContext = systemContext;
        return (A) this;
    }

    public Boolean buffered() {
        return buffered;
    }

    /**
     * @param buffered determine if a channel should be buffered on the other side, or null if left to the server
     * @return this
     */
    public A buffered(Boolean buffered) {
        this.buffered = buffered;
        return (A) this;
    }
}
