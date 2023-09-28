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
 * A specialized {@link ErrorReplyHandler} responsible for delegating operations to a Gateway.
 *
 * <p>This handler is meant to be replaced by a Gateway with a handler that it supports. If no such handler
 * is provided by the Gateway, this handler falls back to returning an error message indicating the absence of a default handler for the Gateway.
 */
public class GatewayHandler extends ErrorReplyHandler {

    /**
     * Constructs a new GatewayHandler instance, initializing the error message of the superclass
     * {@link ErrorReplyHandler} with a message indicating the absence of a default handler for the Gateway.
     */
    public GatewayHandler() {
        errorMsg("No default handler for the gateway");
    }
}
