/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Unit test class extending from WireTestCommon to get basic setup for the Chronicle Wire tests.
@SuppressWarnings({"deprecation", "removal"})
public class StringConsumerMarshallableOutTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Test case to check if serialization to the YAML format works correctly.
    @Test
    @DisplayName("Serialises method calls into yaml lines")
    public void saysYaml() {
        final WireType wireType = WireType.YAML_ONLY; // Specify the wire type as YAML.
        final String expected = // Expected serialized output.
                "say: One\n" +
                "...\n" +
                "say: Two\n" +
                "...\n" +
                "say: Three\n" +
                "...\n";
        assertEquals(expected, doTest(wireType),
                "yaml output should match expected lines for YAML_ONLY wire type");
    }

    // Test case to check if serialization to the JSON format works correctly.
    @Test
    @DisplayName("Serialises method calls into json lines")
    public void saysJson() {
        final WireType wireType = WireType.JSON_ONLY; // Specify the wire type as JSON.
        final String expected = // Expected serialized output.
                "{\"say\":\"One\"}\n" +
                "{\"say\":\"Two\"}\n" +
                "{\"say\":\"Three\"}\n";
        assertEquals(expected, doTest(wireType),
                "json output should match expected lines for JSON_ONLY wire type");
    }

    // Helper method to perform the serialization test.
    private String doTest(WireType wireType) {
        StringWriter sw = new StringWriter(); // StringWriter to hold the serialized data.

        // Create an instance of MarshallableOut which will write to the StringWriter.
        MarshallableOut out = new net.openhft.chronicle.wire.internal.StringConsumerMarshallableOut(s -> {
            sw.append(s); // Append serialized string.
            if (!s.endsWith("\n"))
                sw.append('\n'); // Add newline if not already present.
        }, wireType);

        final Says says = out.methodWriter(Says.class); // Get the method writer for the interface.

        // Call the method to be serialized.
        says.say("One");
        says.say("Two");
        says.say("Three");

        return sw.toString(); // Check if the serialized output matches the expected output.
    }

    // Interface representing a method that can be serialized.
    interface Says {
        void say(String text); // Method to be serialized.
    }
}
