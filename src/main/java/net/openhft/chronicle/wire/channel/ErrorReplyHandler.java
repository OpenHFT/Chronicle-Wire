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

public class ErrorReplyHandler extends AbstractHandler<ErrorReplyHandler> {
    private String errorMsg = "unknown";

    public String errorMsg() {
        return errorMsg;
    }

    public ErrorReplyHandler errorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
        return this;
    }

    @Override
    public void run(ChronicleContext context, ChronicleChannel channel) throws ClosedIORuntimeException {
        try (DocumentContext dc = channel.writingDocument()){
            dc.wire().write("error").text(errorMsg);
        }
    }

    @Override
    public ChronicleChannel asInternalChannel(ChronicleContext context, ChronicleChannelCfg channelCfg) {
        throw new UnsupportedOperationException();
    }
}
