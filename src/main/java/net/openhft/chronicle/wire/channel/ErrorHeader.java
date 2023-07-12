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
 * A specialized type of {@link AbstractHeader} that encapsulates an error message.
 *
 * <p>ErrorHeader instances are used to convey error information between different components of a system
 * via a {@link ChronicleChannel}. The error message can be retrieved and modified using provided getter
 * and setter methods.
 */
public class ErrorHeader extends AbstractHeader<ErrorHeader> {
    private String errorMsg;

    /**
     * Retrieves the error message encapsulated by this header.
     *
     * @return A string representing the stored error message. If no error message has been set, this may return null.
     */
    public String errorMsg() {
        return errorMsg;
    }

    /**
     * Sets the error message to be encapsulated by this header.
     *
     * @param errorMsg A string representing the error message to be stored.
     * This could be the description of an error or an exception message.
     * @return This ErrorHeader instance, to support fluent interface design and method chaining.
     */
    public ErrorHeader errorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
        return this;
    }
}
