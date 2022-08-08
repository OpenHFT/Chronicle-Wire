/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under Contract only
 */

package net.openhft.chronicle.wire.channel;

import net.openhft.chronicle.core.io.ClosedIORuntimeException;
import net.openhft.chronicle.wire.DocumentContext;

/**
 * This handler leaves the default behaviour to the Gateway
 */
public class GatewayHandler extends AbstractHandler<GatewayHandler> {
    @Override
    public void run(ChronicleContext context, ChronicleChannel channel) throws ClosedIORuntimeException {
        try (DocumentContext dc = channel.writingDocument()) {
            dc.wire().write("error").text("No default handler for the gateway");
        }
    }

    @Override
    public ChronicleChannel asInternalChannel(ChronicleContext context, ChronicleChannelCfg channelCfg) {
        throw new UnsupportedOperationException();
    }
}
