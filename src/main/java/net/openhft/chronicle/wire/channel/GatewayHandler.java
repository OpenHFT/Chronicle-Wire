/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under Contract only
 */

package net.openhft.chronicle.wire.channel;

/**
 * This handler leaves the default behaviour to the Gateway.
 * <p>
 * If the gateway doesn't replace it with the Handler the gateway supports, an error is returned
 */
public class GatewayHandler extends ErrorReplyHandler {
    public GatewayHandler() {
        errorMsg("No default handler for the gateway");
    }
}
