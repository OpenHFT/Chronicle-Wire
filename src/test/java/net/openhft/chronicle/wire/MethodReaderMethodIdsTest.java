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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * This test verifies that the MethodReader can identify methods using their IDs.
 */
public class MethodReaderMethodIdsTest extends WireTestCommon {

    /**
     * Test case to verify that method calls can be identified by Method IDs.
     */
    @Test
    public void shouldDetermineMethodNamesFromMethodIds() {
        final BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        wire.usePadding(true);

        // Create a method writer proxy for the Speaker interface
        final Speaker speaker = wire.methodWriterBuilder(Speaker.class).get();

        // Ensure we're not using a proxy instance
        assertFalse("check we are using generated code", Proxy.isProxyClass(speaker.getClass()));

        // Call a method on the proxy
        speaker.say("hello");

        // Counter to track messages heard by the reader
        final AtomicInteger heard = new AtomicInteger();

        // Create a MethodReader instance with a Speaker implementation that increments 'heard'
        final MethodReader reader = new VanillaMethodReaderBuilder(wire).build((Speaker) message -> heard.incrementAndGet());

        // Ensure we're using a generated code instance and not a VanillaMethodReader
        assertFalse("check we are using generated code", reader instanceof VanillaMethodReader);

        // Read one message from the wire
        assertTrue(reader.readOne());

        // Verify the message was "heard"
        assertEquals(1, heard.get());
    }

    // Speaker interface with a method having a specific ID
    interface Speaker {
        @MethodId(7)
        void say(final String message);
    }
}
