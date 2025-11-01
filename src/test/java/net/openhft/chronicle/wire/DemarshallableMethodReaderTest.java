/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.MethodReader;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.IsSame;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import static net.openhft.chronicle.bytes.Bytes.allocateElasticOnHeap;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(Parameterized.class)
public class DemarshallableMethodReaderTest {

    private final Wire wire;

    public DemarshallableMethodReaderTest(Wire wire) {
        this.wire = wire;
        System.out.println("Using wire: " + wire.getClass().getSimpleName());
    }

    @Parameterized.Parameters()
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

    @Test
    public void writesAndReadsMultipleMessages() {
        MessageListener writer = wire.methodWriter(MessageListener.class);
        writer.onMessage(new SelfDescribingDemarshallableObject("msg1", 1.5));
        writer.onMessage(new SelfDescribingDemarshallableObject("msg2", 2.3));

        AtomicReference<SelfDescribingDemarshallableObject> marshalledObjectRef = new AtomicReference<>();

        MessageListener messageListenerAssertion = value -> {
            assertThat(value instanceof SelfDescribingDemarshallableObject, Is.is(true));
            SelfDescribingDemarshallableObject obj = (SelfDescribingDemarshallableObject) value;
            if (marshalledObjectRef.get() == null) {
                marshalledObjectRef.set(obj);
            } else {
                assertThat(obj, not(IsSame.sameInstance(marshalledObjectRef.get())));
            }
            assertThat(obj.name, IsNot.not(nullValue()));
            assertThat(obj.value, IsNot.not(Double.NaN));
            System.out.println("Received message: " + obj.name + ", id: " + obj.value);
        };

        try (MethodReader reader = wire.methodReader(messageListenerAssertion)) {
            reader.readOne();
            reader.readOne();
        }
    }
}