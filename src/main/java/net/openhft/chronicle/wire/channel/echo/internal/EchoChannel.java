package net.openhft.chronicle.wire.channel.echo.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.SimpleCloseable;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.UnrecoverableTimeoutException;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import net.openhft.chronicle.wire.channel.ChannelHeader;
import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleChannelCfg;
import net.openhft.chronicle.wire.channel.OkHeader;
import net.openhft.chronicle.wire.converter.NanoTime;

import java.util.function.Function;

public class EchoChannel extends SimpleCloseable implements ChronicleChannel {

    private static final OkHeader OK = new OkHeader();
    private final Wire wire = WireType.BINARY_LIGHT.apply(Bytes.allocateElasticOnHeap());
    private final ChronicleChannelCfg channelCfg;
    private long lastTestMessage;

    public EchoChannel(ChronicleChannelCfg channelCfg) {
        this.channelCfg = channelCfg;
    }

    @Override
    public ChronicleChannelCfg channelCfg() {
        return channelCfg;
    }

    @Override
    public ChannelHeader headerOut() {
        return OK;
    }

    @Override
    public ChannelHeader headerIn() {
        return OK;
    }

    @Override
    public ChannelHeader headerIn(Function<ChannelHeader, ChannelHeader> redirectFunction) {
        return redirectFunction.apply(headerIn());
    }

    @Override
    public void testMessage(long now) {
        this.lastTestMessage = now;
        try (DocumentContext dc = writingDocument(true)) {
            dc.wire().write("testMessage").writeLong(NanoTime.INSTANCE, now);
        }
    }

    @Override
    public long lastTestMessage() {
        return lastTestMessage;
    }

    @Override
    public DocumentContext readingDocument() {
        return wire.readingDocument();
    }

    @Override
    public DocumentContext writingDocument(boolean metaData) throws UnrecoverableTimeoutException {
        if (wire.isEmpty())
            wire.reset();
        return wire.writingDocument(metaData);
    }

    @Override
    public DocumentContext acquireWritingDocument(boolean metaData) throws UnrecoverableTimeoutException {
        if (wire.isEmpty())
            wire.reset();
        return wire.acquireWritingDocument(metaData);
    }
}
