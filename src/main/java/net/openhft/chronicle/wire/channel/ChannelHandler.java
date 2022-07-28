package net.openhft.chronicle.wire.channel;


import net.openhft.chronicle.core.io.ClosedIORuntimeException;

public interface ChannelHandler extends ChannelHeader {

    default ChannelHeader responseHeader(ChronicleContext context) {
        return new OkHeader();
    }

    void run(ChronicleContext context, ChronicleChannel channel) throws ClosedIORuntimeException;

    default boolean closeWhenRunEnds() {
        return true;
    }

    ChronicleChannel asInternalChannel(ChronicleContext context, ChronicleChannelCfg channelCfg);

    Boolean buffered();
}
