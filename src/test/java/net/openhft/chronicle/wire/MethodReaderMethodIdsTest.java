/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class MethodReaderMethodIdsTest extends WireTestCommon {

    /**
     * Test case to verify that method calls can be identified by Method IDs.
     */
    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("Method ids resolve to method names and reader dispatches")
    public void shouldDetermineMethodNamesFromMethodIds() {
        final BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        wire.usePadding(true);

        // Create a method writer proxy for the Speaker interface
        final Speaker speaker = wire.methodWriterBuilder(Speaker.class).get();

        // Ensure we're not using a proxy instance
        assertFalse(Proxy.isProxyClass(speaker.getClass()),
                "Method writer should be generated code, not a JDK proxy");

        // Call a method on the proxy
        speaker.say("hello");

        // Counter to track messages heard by the reader
        final AtomicInteger heard = new AtomicInteger();

        // Create a MethodReader instance with a Speaker implementation that increments 'heard'
        final MethodReader reader = new VanillaMethodReaderBuilder(wire).build((Speaker) message -> heard.incrementAndGet());

        // Ensure we're using a generated code instance and not a VanillaMethodReader
        assertFalse(reader instanceof VanillaMethodReader,
                "Method reader should be generated code, not VanillaMethodReader");

        // Read one message from the wire
        assertTrue(reader.readOne(), "Reader should process the single written message");

        // Verify the message was "heard"
        assertEquals(1, heard.get(), "Reader should dispatch exactly one message");
    }

    // Speaker interface with a method having a specific ID
    interface Speaker {
        @MethodId(7)
        void say(final String message);
    }
}
