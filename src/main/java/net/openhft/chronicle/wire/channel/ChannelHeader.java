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
 * The ChannelHeader interface extends the Marshallable interface, so it can be passed over a {@link ChronicleChannel}.
 * It provides methods for setting and retrieving the system context
 * and the session name of a channel.
 */
public interface ChannelHeader extends Marshallable {

    /**
     * Returns the system context of the channel.
     *
     * @return the system context as a SystemContext object
     */
    SystemContext systemContext();

    /**
     * Sets the system context of the channel.
     *
     * @param systemContext the new system context to be set
     * @return this instance of the channel header
     */
    ChannelHeader systemContext(SystemContext systemContext);

    /**
     * Returns the session name of the channel.
     *
     * @return the session name as a String
     */
    String sessionName();

    /**
     * Sets the session name of the channel.
     *
     * @param connectionId the new session name to be set
     * @return this instance of the channel header
     */
    ChannelHeader sessionName(String connectionId);
}
