/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Extends WireTestCommon to inherit thread dump and exception monitoring features
public class MarshallableMethodReaderTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Test method to verify the functionality of MethodReader with 'say' method
    @Test
    public void test() {
        // Creates a Wire instance with predefined input
        Wire wire = new TextWire(Bytes.from("say: hi")).useTextDocuments();

        // Initializes SayingMicroservice instance
        final SayingMicroservice sm = new SayingMicroservice();

        // Creates a MethodReader instance for the SayingMicroservice
        final MethodReader reader = wire.methodReader(sm);

        // Asserts that the MethodReader successfully reads a method call
        assertTrue(reader.readOne(), "method reader should successfully read method call from wire");
    }

    // Test for ignoring methods without scanning
    @Test
    public void ignoredMethods() {
        assertTrue(doIgnoredMethods(false), "method reader should process unknown methods when scanning disabled");
    }

    // Test for ignoring methods with scanning
    @Test
    public void ignoredMethodsScanning() {
        assertTrue(doIgnoredMethods(true), "method reader should skip unknown methods when scanning enabled");
    }

    // Helper method to test ignoring methods with or without scanning
    private boolean doIgnoredMethods(boolean scanning) {
        expectException("Unknown method-name='bye' called on class net.openhft.chronicle.wire.method.MarshallableMethodReaderTest$SayingMicroservice");
        // Creates a new YAML based Wire instance
        Wire wire = Wire.newYamlWireOnHeap();

        // Initializes SayingMicroservice instance
        final SayingMicroservice sm = new SayingMicroservice();

        // Configures and builds a MethodReader instance with scanning option
        final MethodReader reader = wire.methodReaderBuilder().scanning(scanning).build(sm);

        // Writes a 'say' method call to the wire and asserts that it is read
        writeDoc(wire, "say");
        assertTrue(reader.readOne(), "method reader should read valid 'say' method call");

        // Writes a 'bye' method call to the wire
        writeDoc(wire, "bye");

        // If not scanning, asserts that the 'bye' method call is read
        if (!scanning)
            assertTrue(reader.readOne(), "method reader should read unknown 'bye' method when not scanning");

        // Asserts that there are no more method calls to read
        assertFalse(reader.readOne(), "method reader should have no more method calls to read");

        // Writes 'bye' and 'say' method calls to the wire and asserts 'say' is read
        writeDoc(wire, "bye");
        writeDoc(wire, "say");
        assertTrue(reader.readOne(), "method reader should read 'say' method after skipping 'bye'");

        // If not scanning, asserts that the next 'bye' method call is read
        if (!scanning)
            assertTrue(reader.readOne(), "method reader should read second 'bye' when not scanning");
        assertFalse(reader.readOne(), "method reader should have no remaining methods after processing all");
        return true;
    }

    // Utility method to write a method call into the wire
    private static void writeDoc(@NotNull Wire wire, String say) {
        try (DocumentContext dc = wire.writingDocument()) {
            dc.wire().write(say).text("");
        }
    }

    // Interface representing a saying action
    interface Saying {
        void say(String hi);
    }

    // Implementation of the Saying interface, stores said strings
    static class SayingMicroservice extends SelfDescribingMarshallable implements Saying {
        final transient List<String> said = new ArrayList<>();

        @Override
        public void say(String hi) {
            // Adds the said string to the list
            said.add(hi);
        }

        // Method not called as it's not declared in the Saying interface
        public void bye(String reason) {
        }
    }
}
