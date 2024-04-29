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
 * This interface represents a channel handler that performs various channel-related operations
 * such as executing actions within a context, handling responses, and configuring channel settings.
 * The ChannelHandler is passed from the client to the gateway or set by the gateway based on context.
 */
@SuppressWarnings("deprecation")
public interface ChannelHandler extends ChannelHeader {

    /**
     * Provides a response header to send back to the client.
     * Default implementation returns an OkHeader instance.
     *
     * @param context the ChronicleContext within which the response header is needed
     * @return a ChannelHeader object that represents the response header
     */
    default ChannelHeader responseHeader(ChronicleContext context) {
        return new OkHeader();
    }

    /**
     * Executes actions within the provided context and channel.
     *
     * @param context the ChronicleContext within which actions will be executed
     * @param channel the ChronicleChannel within which actions will be executed
     * @throws ClosedIORuntimeException     if an I/O error occurs
     * @throws InvalidMarshallableException if a Marshallable object is invalid
     */
    void run(ChronicleContext context, ChronicleChannel channel) throws ClosedIORuntimeException, InvalidMarshallableException;

    /**
     * Determines whether the ChronicleChannel should be closed when the run method ends.
     * If false, the channel will continue to be open after the run method ends, e.g., a subscribe-only channel.
     *
     * @return true if the channel should be closed when the run method ends, false otherwise
     */
    default boolean closeWhenRunEnds() {
        return true;
    }

    /**
     * Converts the current ChannelHandler to an internal channel using the provided context and channel configuration.
     *
     * @param context    the ChronicleContext within which the internal channel is created
     * @param channelCfg the ChronicleChannelCfg used to configure the new internal channel
     * @return a new ChronicleChannel instance
     */
    ChronicleChannel asInternalChannel(ChronicleContext context, ChronicleChannelCfg<?> channelCfg);

    /**
     * Indicates whether the handler supports buffering.
     *
     * @return a Boolean value that indicates if buffering is supported, null if not specified
     */
    Boolean buffered();

    /**
     * @return whether writers to this channel should include the history in the output
     */
    default boolean recordHistory() {
        return false;
    }
}
