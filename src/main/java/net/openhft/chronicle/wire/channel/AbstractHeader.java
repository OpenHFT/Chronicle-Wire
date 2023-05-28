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

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
/**
 * An abstract class that serves as a base for all types of channel headers.
 * This class also includes fields for a system context and a session name, which can be set and retrieved.
 *
 * @param <H> the class extending AbstractHeader, used to allow method chaining
 */
public class AbstractHeader<H extends AbstractHeader<H>>
        extends SelfDescribingMarshallable
        implements ChannelHeader {

    // The system context for this header
    private SystemContext systemContext;

    // The session name for this header
    private String sessionName;

    /**
     * Retrieves the system context of this header.
     *
     * @return the system context as a SystemContext object
     */
    @Override
    public SystemContext systemContext() {
        return systemContext;
    }

    /**
     * Sets the system context of this header.
     *
     * @param systemContext the new system context to be set
     * @return this instance of the header
     */
    @Override
    public H systemContext(SystemContext systemContext) {
        this.systemContext = systemContext;
        return (H) this;
    }

    /**
     * Retrieves the session name of this header.
     *
     * @return the session name as a String
     */
    @Override
    public String sessionName() {
        return sessionName;
    }

    /**
     * Sets the session name of this header.
     *
     * @param sessionName the new session name to be set
     * @return this instance of the header
     */
    @Override
    public H sessionName(String sessionName) {
        this.sessionName = sessionName;
        return (H) this;
    }
}
