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

package net.openhft.chronicle.wire;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A test class for validating the behavior of AbstractCommonMarshallable.
 * This class extends the base test functionalities provided by WireTestCommon.
 */
class AbstractCommonMarshallableTest extends net.openhft.chronicle.wire.WireTestCommon {

    /**
     * Tests the default behavior of AbstractCommonMarshallable
     * to ensure it doesn't use self-describing messages by default.
     */
    @Test
    void doesNotUseSelfDescribingMessagesByDefault() {
        // Assert that a new instance of AbstractCommonMarshallable
        // doesn't use self-describing messages by default
        assertFalse(new AbstractCommonMarshallable() {
        }.usesSelfDescribingMessage());
    }
}
