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
 * Interface InternalChronicleChannel extends ChronicleChannel and defines several additional
 * methods to support advanced functionality within Chronicle Channels.
 * Note: This interface's options might change in future versions of the system.
 */
public interface InternalChronicleChannel extends ChronicleChannel {

    /**
     * This method returns the header to be used for incoming data based on the acceptor's
     * replacement rules.
     *
     * @return the header for incoming data.
     */
    ChannelHeader headerInToUse();

    /**
     * Checks if the event poller is supported by this channel.
     *
     * @return true if event pollers are supported, false otherwise.
     */
    boolean supportsEventPoller();

    /**
     * Gets the EventPoller instance associated with this channel.
     *
     * @return the EventPoller instance set on this channel.
     */
    EventPoller eventPoller();

    /**
     * Associates an EventPoller with this channel to include in any background processing.
     *
     * @param eventPoller the EventPoller instance to set.
     * @return this channel instance, allowing for method chaining.
     */
    ChronicleChannel eventPoller(EventPoller eventPoller);

    /**
     * Acquires a producer instance associated with this channel.
     *
     * @return the producer instance as a WireOut object.
     */
    WireOut acquireProducer();

    /**
     * Releases the producer instance associated with this channel.
     */
    void releaseProducer();

    /**
     * Gets the buffer size used by this channel.
     *
     * @return the buffer size as an integer.
     */
    int bufferSize();
}
