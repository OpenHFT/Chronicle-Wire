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

package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

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
        assertTrue(reader.readOne());
    }

    // Test for ignoring methods without scanning
    @Test
    public void ignoredMethods() {
        doIgnoredMethods(false);
    }

    // Test for ignoring methods with scanning
    @Test
    public void ignoredMethodsScanning() {
        doIgnoredMethods(true);
    }

    // Helper method to test ignoring methods with or without scanning
    public void doIgnoredMethods(boolean scanning) {
        // Creates a new YAML based Wire instance
        Wire wire = Wire.newYamlWireOnHeap();

        // Initializes SayingMicroservice instance
        final SayingMicroservice sm = new SayingMicroservice();

        // Configures and builds a MethodReader instance with scanning option
        final MethodReader reader = wire.methodReaderBuilder().scanning(scanning).build(sm);

        // Writes a 'say' method call to the wire and asserts that it is read
        writeDoc(wire, "say");
        assertTrue(reader.readOne());

        // Writes a 'bye' method call to the wire
        writeDoc(wire, "bye");

        // If not scanning, asserts that the 'bye' method call is read
        if (!scanning)
            assertTrue(reader.readOne());

        // Asserts that there are no more method calls to read
        assertFalse(reader.readOne());

        // Writes 'bye' and 'say' method calls to the wire and asserts 'say' is read
        writeDoc(wire, "bye");
        writeDoc(wire, "say");
        assertTrue(reader.readOne());

        // If not scanning, asserts that the next 'bye' method call is read
        if (!scanning)
            assertTrue(reader.readOne());
        assertFalse(reader.readOne());
    }

    // Utility method to write a method call into the wire
    private static void writeDoc(@NotNull Wire wire, String say) {
        try (DocumentContext dc = wire.writingDocument()) {
            wire.write(say).text("");
        }
    }

    // Interface representing a saying action
    interface Saying {
        void say(String hi);
    }

    // Implementation of the Saying interface, stores said strings
    static class SayingMicroservice extends SelfDescribingMarshallable implements Saying {
        transient List<String> said = new ArrayList<>();

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
