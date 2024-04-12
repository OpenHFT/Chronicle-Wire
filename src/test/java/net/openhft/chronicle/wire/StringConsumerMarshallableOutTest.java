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

import net.openhft.chronicle.wire.internal.StringConsumerMarshallableOut;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

// Unit test class extending from WireTestCommon to get basic setup for the Chronicle Wire tests.
public class StringConsumerMarshallableOutTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Test case to check if serialization to the YAML format works correctly.
    @Test
    public void saysYaml() {
        final WireType wireType = WireType.YAML_ONLY; // Specify the wire type as YAML.
        final String expected = "" + // Expected serialized output.
                "say: One\n" +
                "...\n" +
                "say: Two\n" +
                "...\n" +
                "say: Three\n" +
                "...\n";
        doTest(wireType, expected); // Call the test method with YAML wire type and expected output.
    }

    // Test case to check if serialization to the JSON format works correctly.
    @Test
    public void saysJson() {
        final WireType wireType = WireType.JSON_ONLY; // Specify the wire type as JSON.
        final String expected = "" + // Expected serialized output.
                "{\"say\":\"One\"}\n" +
                "{\"say\":\"Two\"}\n" +
                "{\"say\":\"Three\"}\n";
        doTest(wireType, expected); // Call the test method with JSON wire type and expected output.
    }

    // Helper method to perform the serialization test.
    private void doTest(WireType wireType, String expected) {
        StringWriter sw = new StringWriter(); // StringWriter to hold the serialized data.

        // Create an instance of MarshallableOut which will write to the StringWriter.
        MarshallableOut out = new StringConsumerMarshallableOut(s -> {
            sw.append(s); // Append serialized string.
            if (!s.endsWith("\n"))
                sw.append('\n'); // Add newline if not already present.
        }, wireType);

        final Says says = out.methodWriter(Says.class); // Get the method writer for the interface.

        // Call the method to be serialized.
        says.say("One");
        says.say("Two");
        says.say("Three");

        assertEquals(expected, sw.toString()); // Check if the serialized output matches the expected output.
    }

    // Interface representing a method that can be serialized.
    interface Says {
        void say(String text); // Method to be serialized.
    }
}
