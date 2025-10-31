/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

// Import necessary libraries...

/**
 * Test suite for ignoring unknown datetime during deserialization.
 */
public class UnknownDatatimeTest extends WireTestCommon {

    /**
     * Tests if an unknown datetime can be ignored during deserialization.
     */
    @Test
    public void ignoreAnUnknownDateTime() throws IOException {
        // Deserialize an instance of AClass from a string.
        // The string contains a datetime field 'eventTime' which is not expected to exist in AClass.
        AClass aClass = Marshallable.fromString("!" + AClass.class.getName() + " { eventTime: 2019-04-02T11:20:41.616653, id: 123456 }");

        // Assert that the 'id' field of the deserialized object has the expected value.
        // The absence of 'eventTime' in AClass should not cause any issues during deserialization.
        assertEquals(123456, aClass.id);
    }
}
