/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.MethodReader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import static net.openhft.chronicle.bytes.Bytes.allocateElasticOnHeap;
import static org.junit.jupiter.api.Assertions.*;

public class DemarshallableMethodReaderTest {

    private Wire wire;

    public void initDemarshallableMethodReaderTest(Wire wire) {
        this.wire = wire;
        System.out.println("Using wire: " + wire.getClass().getSimpleName());
    }

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
         * Handles an incoming message.
         *
         * @param value the message payload
         */
        void onMessage(Object value);
    }

    @MethodSource("combinations")
    @ParameterizedTest()
    public void writesAndReadsMultipleMessages(Wire wire) {
        initDemarshallableMethodReaderTest(wire);
        MessageListener writer = wire.methodWriter(MessageListener.class);
        writer.onMessage(new SelfDescribingDemarshallableObject("msg1", 1.5));
        writer.onMessage(new SelfDescribingDemarshallableObject("msg2", 2.3));

        AtomicReference<SelfDescribingDemarshallableObject> marshalledObjectRef = new AtomicReference<>();

        MessageListener messageListenerAssertion = value -> {
            assertInstanceOf(SelfDescribingDemarshallableObject.class, value, "message type");
            SelfDescribingDemarshallableObject obj = (SelfDescribingDemarshallableObject) value;
            if (marshalledObjectRef.get() == null) {
                marshalledObjectRef.set(obj);
            } else {
                assertNotSame(marshalledObjectRef.get(), obj, "message instance");
            }
            assertNotNull(obj.name, "message name");
            assertFalse(Double.isNaN(obj.value), "message value is NaN");
            System.out.println("Received message: " + obj.name + ", id: " + obj.value);
        };

        try (MethodReader reader = wire.methodReader(messageListenerAssertion)) {
            assertTrue(reader.readOne(), "first message");
            assertTrue(reader.readOne(), "second message");
            assertFalse(reader.readOne(), "no more messages");
        }
    }
}
