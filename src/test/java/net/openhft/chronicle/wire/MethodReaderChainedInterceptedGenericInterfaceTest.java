/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class tests the capability of the MethodReader to handle parametrized interfaces hierarchy and chained calls.
 * This pattern arises when using Web Gateway.
 */
public class MethodReaderChainedInterceptedGenericInterfaceTest extends WireTestCommon {
    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("Definitive interface chain reads in order")
    public void testDefinitive() {
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));
        wire.usePadding(true);

        final DefinitiveIface writer = wire.methodWriterBuilder(DefinitiveIface.class)
                .updateInterceptor((methodName, t) -> true).build();

        writer.ride("train");
        writer.destination("Germany").call("Buchloe");
        writer.destination("Belgium").call("Liege");

        MethodReader methodReader = wire.methodReader(new DefinitiveIface() {
            @Override
            public void ride(String what) {
                assertEquals("train", what, "ride should receive the expected transport");
            }

            @Override
            public Endpoint destination(String target) {
                return result -> {
                    switch (target) {
                        case "Germany":
                            assertEquals("Buchloe", result, "Destination result should match for Germany");
                            break;
                        case "Belgium":
                            assertEquals("Liege", result, "Destination result should match for Belgium");
                            break;
                        default:
                            fail("Definitive destination target should be Germany or Belgium, got " + target);
                    }
                };
            }
        });

        assertTrue(methodReader.readOne(), "definitive reader should read event 0");
        assertTrue(methodReader.readOne(), "definitive reader should read event 1");
        assertTrue(methodReader.readOne(), "definitive reader should read event 2");
        assertFalse(methodReader.readOne(), "definitive reader should report no more events");
    }

    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("Indefinite interface chain reads in order")
    public void testIndefinite() {
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));
        wire.usePadding(true);

        final IndefiniteIface writer = wire.methodWriterBuilder(IndefiniteIface.class)
                .updateInterceptor((methodName, t) -> true).build();

        writer.fly("plane");
        writer.destination("UK").call("London");
        writer.destination("USA").call("Miami");

        MethodReader methodReader = wire.methodReader(new IndefiniteIface() {
            @Override
            public void fly(String what) {
                assertEquals("plane", what, "indefinite fly should receive expected transport value");
            }

            @Override
            public Endpoint destination(String target) {
                return result -> {
                    switch (target) {
                        case "UK":
                            assertEquals("London", result, "Destination result should match for UK");
                            break;
                        case "USA":
                            assertEquals("Miami", result, "Destination result should match for USA");
                            break;
                        default:
                            fail("Indefinite destination target should be UK or USA, got " + target);
                    }
                };
            }
        });

        assertTrue(methodReader.readOne(), "indefinite reader should read event 0");
        assertTrue(methodReader.readOne(), "indefinite reader should read event 1");
        assertTrue(methodReader.readOne(), "indefinite reader should read event 2");
        assertFalse(methodReader.readOne(), "indefinite reader should report no more events");
    }

    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("Nested interface chain reads in order")
    public void testNested() {
        BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap(128));
        wire.usePadding(true);

        final NestedInterface writer = wire.methodWriterBuilder(NestedInterface.class)
                .updateInterceptor((methodName, t) -> true).build();

        writer.move("T2");
        writer.fly("plane");
        writer.destination("Chile").call("Santiago");
        writer.destination("Peru").call("Lima");

        MethodReader methodReader = wire.methodReader(new NestedInterface() {
            @Override
            public void move(String where) {
                assertEquals("T2", where, "move should receive the expected location");
            }

            @Override
            public void fly(String what) {
                assertEquals("plane", what, "nested fly should receive expected transport value");
            }

            @Override
            public Endpoint destination(String target) {
                return result -> {
                    switch (target) {
                        case "Chile":
                            assertEquals("Santiago", result, "Destination result should match for Chile");
                            break;
                        case "Peru":
                            assertEquals("Lima", result, "Destination result should match for Peru");
                            break;
                        default:
                            fail("Nested destination target should be Chile or Peru, got " + target);
                    }
                };
            }
        });

        assertTrue(methodReader.readOne(), "nested reader should read event 0");
        assertTrue(methodReader.readOne(), "nested reader should read event 1");
        assertTrue(methodReader.readOne(), "nested reader should read event 2");
        assertTrue(methodReader.readOne(), "nested reader should read event 3");
        assertFalse(methodReader.readOne(), "nested reader should report no more events");
    }

    /**
     * Interface resembling QWG's transport for chained calls.
     */
    interface Transport<T> {
        T destination(String target);
    }

    interface Endpoint {
        void call(String result);
    }

    /**
     * Interface which extends Transport and clarifies its method.
     */
    interface DefinitiveIface extends Transport<Endpoint> {
        void ride(String what);

        @Override
        Endpoint destination(String target);
    }

    /**
     * Interface which extends Transport but does not clarify its method.
     */
    interface IndefiniteIface extends Transport<Endpoint> {
        void fly(String what);
    }

    /**
     * Interface which non-generically extends another interface extending Transport.
     */
    interface NestedInterface extends IndefiniteIface {
        void move(String where);
    }
}
