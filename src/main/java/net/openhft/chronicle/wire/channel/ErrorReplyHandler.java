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
 * This class handles replies containing error messages.
 * It extends ReplyingHandler and overrides its responseHeader method
 * to return an ErrorHeader populated with the current error message.
 */
public class ErrorReplyHandler extends ReplyingHandler<ErrorReplyHandler> {
    private String errorMsg = "unknown";

    /**
     * Get the current error message.
     *
     * @return A String representing the current error message.
     */
    public String errorMsg() {
        return errorMsg;
    }

    /**
     * Set the error message for this handler.
     *
     * @param errorMsg A String representing the error message to be set.
     * @return This ErrorReplyHandler instance for method chaining.
     */
    public ErrorReplyHandler errorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
        return this;
    }

    /**
     * Create a response header with the current error message.
     * This method overrides the responseHeader method from ReplyingHandler.
     *
     * @param context A ChronicleContext object. This parameter is not used in this implementation.
     * @return A ChannelHeader object, which is an instance of ErrorHeader populated with the current error message.
     */
    @Override
    public ChannelHeader responseHeader(ChronicleContext context) {
        return new ErrorHeader().errorMsg(errorMsg);
    }
}
