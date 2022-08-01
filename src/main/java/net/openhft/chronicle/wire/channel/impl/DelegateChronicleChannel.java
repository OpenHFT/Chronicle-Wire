package net.openhft.chronicle.wire.channel.impl;

import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.UnrecoverableTimeoutException;
import net.openhft.chronicle.wire.channel.*;

import java.util.function.Function;

public class DelegateChronicleChannel implements InternalChronicleChannel, Closeable {
    protected final InternalChronicleChannel channel;

    public DelegateChronicleChannel(InternalChronicleChannel channel) {
        this.channel = channel;
    }

    @Override
    public ChronicleChannelCfg channelCfg() {
        return channel.channelCfg();
    }

    @Override
    public ChannelHeader headerOut() {
        return channel.headerOut();
    }

    @Override
    public ChannelHeader headerIn() {
        return channel.headerIn();
    }

    @Override
    public ChannelHeader headerIn(Function<ChannelHeader, ChannelHeader> redirectFunction) {
        return channel.headerIn(redirectFunction);
    }

    @Override
    public void testMessage(long now) {
        channel.testMessage(now);
    }

    @Override
    public long lastTestMessage() {
        return channel.lastTestMessage();
    }

    @Override
    public void close() {
        Closeable.closeQuietly(channel);
    }

    @Override
    public boolean isClosed() {
        return channel == null || channel.isClosed();
    }

    @Override
    public DocumentContext readingDocument() {
        return channel.readingDocument();
    }

    @Override
    public DocumentContext writingDocument(boolean metaData) throws UnrecoverableTimeoutException {
        return channel.writingDocument(metaData);
    }

    @Override
    public DocumentContext acquireWritingDocument(boolean metaData) throws UnrecoverableTimeoutException {
        return channel.acquireWritingDocument(metaData);
    }

    @Override
    public boolean supportsEventPoller() {
        return channel.supportsEventPoller();
    }

    @Override
    public EventPoller eventPoller() {
        return channel.eventPoller();
    }

    @Override
    public ChronicleChannel eventPoller(EventPoller eventPoller) {
        channel.eventPoller(eventPoller);
        return this;
    }
}
