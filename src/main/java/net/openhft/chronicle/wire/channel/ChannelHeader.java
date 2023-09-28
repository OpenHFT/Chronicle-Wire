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

import net.openhft.chronicle.wire.Marshallable;

/**
 * ChannelHeader represents the metadata of a {@link ChronicleChannel}. It maintains a system context
 * and a session name for a channel, facilitating system-specific and session-specific configurations.
 * <p>
 * This interface extends Marshallable, making it possible for a ChannelHeader instance to be marshalled
 * and unmarshalled for communication over a ChronicleChannel.
 */
public interface ChannelHeader extends Marshallable {

    /**
     * Fetches the associated system context of the channel.
     *
     * @return The SystemContext object representing the system context of the channel.
     */
    SystemContext systemContext();

    /**
     * Assigns a new system context to the channel.
     *
     * @param systemContext The new SystemContext object to be set as the system context.
     * @return The current ChannelHeader instance, allowing for method chaining.
     */
    ChannelHeader systemContext(SystemContext systemContext);

    /**
     * Fetches the session name of the channel.
     *
     * @return The session name of the channel as a String.
     */
    String sessionName();

    /**
     * Assigns a new session name to the channel.
     *
     * @param sessionName The new session name to be assigned to the channel.
     * @return The current ChannelHeader instance, allowing for method chaining.
     */
    ChannelHeader sessionName(String sessionName);
}
