/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.MethodReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import static net.openhft.chronicle.bytes.Bytes.allocateElasticOnHeap;
import static org.junit.jupiter.api.Assertions.*;

public class DemarshallableMethodReaderTest {

    public static Collection<Wire> combinations() {
        return Arrays.asList(
                new BinaryWire(allocateElasticOnHeap()),
                Wire.newYamlWireOnHeap(),
                new TextWire(allocateElasticOnHeap())
        );
    }

    /**
     * Listener interface for receiving messages. Uses {@code Object} as the
     * parameter type to allow flexibility in message payload types.
     */
    public interface MessageListener {
        /**
         * Handles an incoming message payload from the wire.
         *
         * @param value the message payload
         */
        void onMessage(Object value);
    }

    @MethodSource("combinations")
    @ParameterizedTest
    @DisplayName("Writes and reads multiple demarshallable messages")
    public void writesAndReadsMultipleMessages(Wire wire) {
        MessageListener writer = wire.methodWriter(MessageListener.class);
        writer.onMessage(new SelfDescribingDemarshallableObject("msg1", 1.5));
        writer.onMessage(new SelfDescribingDemarshallableObject("msg2", 2.3));

        AtomicReference<SelfDescribingDemarshallableObject> marshalledObjectRef = new AtomicReference<>();

        MessageListener messageListenerAssertion = value -> {
            assertInstanceOf(SelfDescribingDemarshallableObject.class, value,
                    "message payload should be SelfDescribingDemarshallableObject for wire " + wire.getClass().getSimpleName());
            SelfDescribingDemarshallableObject obj = (SelfDescribingDemarshallableObject) value;
            if (marshalledObjectRef.get() == null) {
                marshalledObjectRef.set(obj);
            } else {
                assertNotSame(marshalledObjectRef.get(), obj,
                        "each message should be a distinct object instance for wire " + wire.getClass().getSimpleName());
            }
            assertNotNull(obj.name,
                    "message name should be present for wire " + wire.getClass().getSimpleName());
            assertFalse(Double.isNaN(obj.value),
                    "message value should not be NaN for wire " + wire.getClass().getSimpleName());
            System.out.println("Received message: " + obj.name + ", id: " + obj.value);
        };

        try (MethodReader reader = wire.methodReader(messageListenerAssertion)) {
            assertTrue(reader.readOne(),
                    "first message should be read for wire " + wire.getClass().getSimpleName());
            assertTrue(reader.readOne(),
                    "second message should be read for wire " + wire.getClass().getSimpleName());
            assertFalse(reader.readOne(),
                    "no more messages should remain for wire " + wire.getClass().getSimpleName());
        }
    }
}
