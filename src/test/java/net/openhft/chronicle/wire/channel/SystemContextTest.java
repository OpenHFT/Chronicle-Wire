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

public class SystemContextTest extends net.openhft.chronicle.wire.WireTestCommon {

    @Test
    public void availableProcessors() {
        assertTrue(INSTANCE.availableProcessors() > 0);
    }

    @Test
    public void hostId() {
        assertTrue(INSTANCE.hostId() >= 0);
    }

    @Test
    public void hostName() {
        final String hostName = INSTANCE.hostName();
        assertNotEquals("localhost", hostName);
        try {
            // test if its a docker hostname
            Long.parseUnsignedLong(hostName, 16);
        } catch (NumberFormatException nfe) {
            assertTrue(hostName, hostName.matches("[a-zA-Z].*"));
        }
    }

    @Test
    public void upTime() {
        assertTrue(INSTANCE.upTime() > 1.6e18);
        assertTrue(INSTANCE.upTime() < 2e18);
    }

    @Test
    public void userCountry() {
        assertTrue(INSTANCE.userCountry().matches("[a-zA-Z].*"));
    }

    @Test
    public void userName() {
        assertTrue(INSTANCE.userName().matches("[a-zA-Z].*"));
    }
}
