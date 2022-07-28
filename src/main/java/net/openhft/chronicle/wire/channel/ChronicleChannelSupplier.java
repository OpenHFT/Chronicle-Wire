package net.openhft.chronicle.wire.channel;

import java.util.function.Supplier;

public class ChronicleChannelSupplier extends ChronicleChannelCfg<ChronicleChannelSupplier> implements Supplier<ChronicleChannel> {
    private final ChronicleContext context;
    private final ChannelHandler handler;
    private String protocol;

    public ChronicleChannelSupplier(ChronicleContext context, ChannelHandler handler) {
        this.context = context;
        this.handler = handler;
    }

    @Override
    public ChronicleChannel get() {
        handler.systemContext(context.systemContext());
        final ChronicleChannel channel;
        switch (protocol) {
            case "tcp":
                channel = ChronicleChannel.newChannel(context.socketRegistry(), this, handler);
                break;
            case "internal":
                channel = handler.asInternalChannel(context, this);
                break;
            default:
                throw new IllegalArgumentException("Unsupported protocol " + protocol);
        }
        context.addCloseable(channel);
        return channel;
    }

    public String protocol() {
        return protocol;
    }

    public ChronicleChannelSupplier protocol(String protocol) {
        this.protocol = protocol;
        return this;
    }
}
