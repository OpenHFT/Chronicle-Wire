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
 * This class represents a header that contains an error message.
 * It extends AbstractHeader and provides methods to get and set the error message.
 */
public class ErrorHeader extends AbstractHeader<ErrorHeader> {
    private String errorMsg;

    /**
     * Get the error message stored in the header.
     *
     * @return A String representing the error message.
     */
    public String errorMsg() {
        return errorMsg;
    }

    /**
     * Set the error message to be stored in the header.
     *
     * @param errorMsg A String representing the error message to be set.
     * @return This ErrorHeader instance for method chaining.
     */
    public ErrorHeader errorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
        return this;
    }
}
