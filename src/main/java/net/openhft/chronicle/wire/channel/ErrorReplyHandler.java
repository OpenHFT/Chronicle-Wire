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
import net.openhft.chronicle.wire.ReplyingHandler;

public class ErrorReplyHandler extends ReplyingHandler<ErrorReplyHandler> {
    private String errorMsg = "unknown";

    public String errorMsg() {
        return errorMsg;
    }

    public ErrorReplyHandler errorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
        return this;
    }

    @Override
    public ChannelHeader responseHeader(ChronicleContext context) {
        return new ErrorHeader().errorMsg(errorMsg);
    }
}
