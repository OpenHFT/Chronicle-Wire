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


import net.openhft.chronicle.core.io.ClosedIORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;

/**
 * An interface representing a channel handler passed from the client to the gateway or set by the gateway based on context.
 * The handler is responsible for managing channel-level operations such as running actions within a context,
 * handling responses, and configuring channel settings.
 */
public interface ChannelHandler extends ChannelHeader {

    /**
     * Provides a response header to return to the client.
     * Default implementation returns an OkHeader.
     *
     * @param context the context for which the response header is needed
     * @return a response header object
     */
    default ChannelHeader responseHeader(ChronicleContext context) {
        return new OkHeader();
    }

    /**
     * Runs actions within a given context and channel.
     *
     * @param context the context within which to run the actions
     * @param channel the channel within which to run the actions
     * @throws ClosedIORuntimeException     if any I/O error occurs
     * @throws InvalidMarshallableException if a Marshallable object is invalid
     */
    void run(ChronicleContext context, ChronicleChannel channel) throws ClosedIORuntimeException, InvalidMarshallableException;

    /**
     * @return true if the {@link ChronicleChannel} should be closed when the run method ends, or false if it should continue to be open, e.g. a subscribe only channel.
     */
    default boolean closeWhenRunEnds() {
        return true;
    }

    /**
     * Converts the current ChannelHandler to an internal channel with the given context and channel configuration.
     *
     * @param context    the context within which to create the internal channel
     * @param channelCfg the configuration for the new internal channel
     * @return a new ChronicleChannel instance
     */
    ChronicleChannel asInternalChannel(ChronicleContext context, ChronicleChannelCfg channelCfg);

    /**
     * Checks whether the handler supports buffering or not.
     *
     * @return a Boolean value, can be null if not specified
     */
    Boolean buffered();

    /**
     * @return whether writers to this channel should include the history in the output
     */
    default boolean recordHistory() {
        return false;
    }
}
