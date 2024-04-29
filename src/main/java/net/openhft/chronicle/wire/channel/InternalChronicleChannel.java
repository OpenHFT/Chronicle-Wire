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

import net.openhft.chronicle.wire.WireOut;

/**
 * The InternalChronicleChannel interface extends the ChronicleChannel interface and provides
 * additional methods that support advanced functionalities within Chronicle Channels.
 * This interface is intended for internal use within the system and its options might change
 * in future versions, hence it should be used with caution.
 */
@SuppressWarnings("deprecation")
public interface InternalChronicleChannel extends ChronicleChannel {

    /**
     * Returns the ChannelHeader to be used for incoming data. The header is determined based on the
     * acceptor's replacement rules.
     *
     * @return the ChannelHeader for incoming data.
     */
    ChannelHeader headerInToUse();

    /**
     * Determines if the current channel supports the EventPoller mechanism for polling events.
     *
     * @return true if event pollers are supported by this channel, false otherwise.
     */
    boolean supportsEventPoller();

    /**
     * Retrieves the EventPoller instance associated with this channel. This EventPoller
     * can be used to poll events from the channel.
     *
     * @return the EventPoller instance associated with this channel.
     */
    EventPoller eventPoller();

    /**
     * Associates a given EventPoller instance with this channel. This EventPoller will be included
     * in any background processing associated with the channel.
     *
     * @param eventPoller the EventPoller instance to associate with this channel.
     * @return this channel instance, allowing for method chaining.
     */
    ChronicleChannel eventPoller(EventPoller eventPoller);

    /**
     * Retrieves a producer instance associated with this channel. This producer can be used
     * to write data to the channel.
     *
     * @return the producer instance as a WireOut object.
     */
    WireOut acquireProducer();

    /**
     * Releases the producer instance previously acquired with acquireProducer(). This method should
     * be called when the producer instance is no longer needed, to free up resources.
     */
    void releaseProducer();

    /**
     * Returns the size of the buffer used by this channel. The buffer size can impact the performance
     * of the channel, with larger buffers typically providing better throughput at the cost of
     * increased memory usage.
     *
     * @return the size of the buffer used by this channel, in bytes.
     */
    int bufferSize();
}
