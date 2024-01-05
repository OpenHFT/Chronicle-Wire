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

import org.junit.Test;

import static net.openhft.chronicle.wire.channel.SystemContext.INSTANCE;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * SystemContextTest class, testing various system properties and behaviors.
 * Extends WireTestCommon from the OpenHFT Chronicle Wire library.
 */
public class SystemContextTest extends net.openhft.chronicle.wire.WireTestCommon {

    @Test
    public void availableProcessors() {
        assertTrue(INSTANCE.availableProcessors() > 0);
    }

    /**
     * Tests the retrieval of the host ID.
     */
    @Test
    public void hostId() {
        // Ensure the host ID is non-negative
        assertTrue(INSTANCE.hostId() >= 0);
    }

    /**
     * Tests the retrieval and validity of the host name.
     */
    @Test
    public void hostName() {
        final String hostName = INSTANCE.hostName();

        // Ensure the host name is not "localhost"
        assertNotEquals("localhost", hostName);

        try {
            // test if its a docker hostname
            Long.parseUnsignedLong(hostName, 16);
        } catch (NumberFormatException nfe) {
            // Ensure the host name starts with an alphabetical character
            assertTrue(hostName, hostName.matches("[a-zA-Z].*"));
        }
    }

    /**
     * Tests the system's uptime value.
     */
    @Test
    public void upTime() {
        // Ensure the system's uptime is within a plausible range
        assertTrue(INSTANCE.upTime() > 1.6e18);
        assertTrue(INSTANCE.upTime() < 2e18);
    }

    /**
     * Tests the retrieval of the user's country.
     */
    @Test
    public void userCountry() {
        // Ensure the user country starts with an alphabetical character
        assertTrue(INSTANCE.userCountry().matches("[a-zA-Z].*"));
    }

    /**
     * Tests the retrieval of the user's name.
     */
    @Test
    public void userName() {
        // Ensure the user name starts with an alphabetical character
        assertTrue(INSTANCE.userName().matches("[a-zA-Z].*"));
    }
}
