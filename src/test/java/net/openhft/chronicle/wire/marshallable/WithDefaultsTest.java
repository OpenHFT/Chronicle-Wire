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

package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

/**
 * Test class for the WithDefaults functionality.
 * Extends the WireTestCommon to leverage utilities related to wire tests.
 */
public class WithDefaultsTest extends WireTestCommon {

    /**
     * Tests the writeMarshallable functionality of WithDefaults under various scenarios.
     */
    @Test
    public void writeMarshallable() {
        // Default test without any modification
        doTest(w -> {
        });

        // Test the scenario after clearing the bytes data
        doTest(w -> w.bytes.clear());

        // Test with changing the default text value
        doTest(w -> w.text = "bye");

        // Test with changing the flag value to false
        doTest(w -> w.flag = false);

        // Test with changing the default numerical value
        doTest(w -> w.num = 5);
    }

    /**
     * Utility function to perform tests on WithDefaults.
     * Initializes an instance, applies the consumer action, and then validates the string
     * representation and object equality.
     *
     * @param consumer Consumer action to apply on the WithDefaults instance.
     */
    void doTest(Consumer<WithDefaults> consumer) {
        // Initialize the WithDefaults instance
        WithDefaults wd = new WithDefaults();

        // Apply the consumer action on the instance
        consumer.accept(wd);

        // Convert the instance to its string representation
        String cs = wd.toString();

        // Convert the string representation back to a WithDefaults object
        WithDefaults o = Marshallable.fromString(cs);

        // Validate the string representation remains consistent
        assertEquals(cs, o.toString());

        // Validate the original and recreated objects are equal
        assertEquals(wd, o);
    }
}
