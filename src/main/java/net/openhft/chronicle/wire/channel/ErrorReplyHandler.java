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
 * A specialized {@link ReplyingHandler} designed to manage error responses.
 *
 * <p>This handler maintains an error message, which can be set and retrieved through dedicated methods. When generating
 * a response header, it produces an {@link ErrorHeader} instance populated with the current error message.
 */
public class ErrorReplyHandler extends ReplyingHandler<ErrorReplyHandler> {
    private String errorMsg = "unknown";

    /**
     * Retrieves the current error message set for this handler.
     *
     * @return A string representing the current error message.
     */
    public String errorMsg() {
        return errorMsg;
    }

    /**
     * Sets the error message for this handler.
     *
     * @param errorMsg The error message to be assigned to this handler.
     * @return This ErrorReplyHandler instance, facilitating method chaining.
     */
    public ErrorReplyHandler errorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
        return this;
    }

    /**
     * Creates an {@link ErrorHeader} as a response, populated with the current error message.
     * Overrides the responseHeader method from the {@link ReplyingHandler} superclass.
     *
     * @param context A {@link ChronicleContext} object. Not used in this implementation.
     * @return A {@link ChannelHeader} object, specifically an instance of {@link ErrorHeader}, populated with the current error message.
     */
    @Override
    public ChannelHeader responseHeader(ChronicleContext context) {
        return new ErrorHeader().errorMsg(errorMsg);
    }
}
