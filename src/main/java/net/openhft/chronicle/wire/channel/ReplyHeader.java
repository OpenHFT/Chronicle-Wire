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
 * Class ReplyHeader extends the AbstractHeader class and encapsulates a reply object of type {@code R}.
 * This class is typically used to include a response object in a message header.
 *
 * @param <R> the type of the reply object this header carries.
 */
public class ReplyHeader<R> extends AbstractHeader<ReplyHeader<R>> {

    // The reply object.
    private final R reply;

    /**
     * Constructs a new ReplyHeader instance with the specified reply object.
     *
     * @param reply the reply object to be stored in the header.
     */
    public ReplyHeader(R reply) {
        this.reply = reply;
    }

    /**
     * Retrieves the reply object stored in the header.
     *
     * @return the reply object of type {@code R}.
     */
    public R replay() {
        return reply;
    }
}
