/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under Contract only
 */

package net.openhft.chronicle.wire.channel.echo;

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.io.ClosedIORuntimeException;
import net.openhft.chronicle.wire.channel.AbstractHandler;
import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleChannelCfg;
import net.openhft.chronicle.wire.channel.ChronicleContext;

public class ChannelVisitorHandler extends AbstractHandler<ChannelVisitorHandler> {
    @Override
    public void run(ChronicleContext context, ChronicleChannel channel) throws ClosedIORuntimeException {
        Replies replies = channel.methodWriter(Replies.class);
        ChannelVisiting visiting = visitor -> replies.reply(visitor.visit(channel));
        channel.eventHandlerAsRunnable(visiting).run();
    }

    @Override
    public ChronicleChannel asInternalChannel(ChronicleContext context, ChronicleChannelCfg channelCfg) {
        throw new UnsupportedOperationException();
    }

}
