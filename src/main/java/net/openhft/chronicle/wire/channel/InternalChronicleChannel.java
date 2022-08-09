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
 * Advanced options that may change in the future
 */
public interface InternalChronicleChannel extends ChronicleChannel {
    /**
     * The header in to use based on the acceptors replacement rules.
     */
    ChannelHeader headerInToUse();

    /**
     * @return true if eventPollers are supported by this Channel.
     */
    boolean supportsEventPoller();

    /**
     * @return the EventPoller set
     */
    EventPoller eventPoller();

    /**
     * Set an EventPoller to include in any background processing.
     *
     * @param eventPoller to use
     * @return this
     */
    ChronicleChannel eventPoller(EventPoller eventPoller);
}
