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
 * The AbstractHandler class serves as a foundational template for all types of channel handlers.
 * It extends the AbstractHeader class and implements the ChannelHandler interface.
 * This class includes a field for indicating whether a channel should be buffered,
 * with corresponding getter and setter methods.
 *
 * @param <H> The type of the class that extends AbstractHandler.
 *            This is used to ensure the correct type is returned by the builder methods, allowing method chaining.
 */
public abstract class AbstractHandler<H extends AbstractHandler<H>>
        extends AbstractHeader<H>
        implements ChannelHandler {

    // A flag to indicate whether or not a channel should be buffered
    private Boolean buffered;

    /**
     * Retrieves the buffer status of the channel associated with this handler.
     *
     * @return Boolean value indicating whether the channel is set to be buffered.
     * Returns null if no preference has been set, which implies default server settings should be used.
     */
    public Boolean buffered() {
        return buffered;
    }

    /**
     * Sets the buffer status of the channel associated with this handler.
     *
     * @param buffered A Boolean value indicating whether the channel should be buffered.
     *                 Set to null to leave the decision to the server.
     * @return This instance of the handler, enabling method chaining.
     */
    public H buffered(Boolean buffered) {
        this.buffered = buffered;
        return (H) this;
    }
}
