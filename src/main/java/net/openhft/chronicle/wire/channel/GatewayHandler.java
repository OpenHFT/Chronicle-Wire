/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.channel;

/**
 * GatewayHandler is a specific implementation of the ErrorReplyHandler.
 * <p>
 * This handler delegates the default behaviour to the Gateway. If the Gateway
 * doesn't replace this handler with a handler it supports, an error message is returned.
 * The error message will indicate that there is no default handler provided by the Gateway.
 */
public class GatewayHandler extends ErrorReplyHandler {

    /**
     * Constructor for GatewayHandler.
     * <p>
     * Initializes the errorMsg in the parent class (ErrorReplyHandler) with
     * a message indicating the absence of a default handler for the Gateway.
     */
    public GatewayHandler() {
        errorMsg("No default handler for the gateway");
    }
}
