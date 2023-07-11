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
 * The AbstractHeader class serves as a foundational template for all channel header types.
 * The class extends the SelfDescribingMarshallable and implements the ChannelHeader interface.
 * It includes fields for a system context and a session name, providing methods to get and set these fields.
 * This class is designed to be extended by other header types to standardize their functionality.
 *
 * @param <H> Type of the class that extends AbstractHeader. Used to ensure the correct type is returned by the builder methods, supporting method chaining.
 */
public abstract class AbstractHeader<H extends AbstractHeader<H>>
        extends SelfDescribingMarshallable
        implements ChannelHeader {

    // System context associated with this header
    private SystemContext systemContext;

    // Session name associated with this header
    private String sessionName;

    /**
     * Retrieves the system context associated with this header.
     *
     * @return the system context as a SystemContext object. Null if no system context has been set.
     */
    @Override
    public SystemContext systemContext() {
        return systemContext;
    }

    /**
     * Sets the system context associated with this header.
     *
     * @param systemContext the SystemContext object to set as the system context for this header.
     * @return this instance of the header, allowing method chaining.
     */
    @Override
    public H systemContext(SystemContext systemContext) {
        this.systemContext = systemContext;
        return (H) this;
    }

    /**
     * Retrieves the session name associated with this header.
     *
     * @return the session name as a String. Null if no session name has been set.
     */
    @Override
    public String sessionName() {
        return sessionName;
    }

    /**
     * Sets the session name associated with this header.
     *
     * @param sessionName the String to set as the session name for this header.
     * @return this instance of the header, allowing method chaining.
     */
    @Override
    public H sessionName(String sessionName) {
        this.sessionName = sessionName;
        return (H) this;
    }
}

