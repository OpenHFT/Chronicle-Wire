/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class tests the capability of the MethodReader to handle parametrized interfaces hierarchy and chained calls.
 * This pattern arises when using Web Gateway.
 */
class MethodReaderChainedInterceptedGenericInterfaceTest extends WireTestCommon {
    @SuppressWarnings("deprecation")
    @Test
    void testDefinitive() {
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
                assertEquals("train", what);
            }

            @Override
            public Endpoint destination(String target) {
                return result -> {
                    switch (target) {
                        case "Germany":
                            assertEquals("Buchloe", result);
                            break;
                        case "Belgium":
                            assertEquals("Liege", result);
                            break;
                        default:
                            fail();
                    }
                };
            }
        });

        methodReader.readOne();
        methodReader.readOne();
        methodReader.readOne();
    }

    @SuppressWarnings("deprecation")
    @Test
    void testIndefinite() {
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
                assertEquals("plane", what);
            }

            @Override
            public Endpoint destination(String target) {
                return result -> {
                    switch (target) {
                        case "UK":
                            assertEquals("London", result);
                            break;
                        case "USA":
                            assertEquals("Miami", result);
                            break;
                        default:
                            fail();
                    }
                };
            }
        });

        methodReader.readOne();
        methodReader.readOne();
        methodReader.readOne();
    }

    @SuppressWarnings("deprecation")
    @Test
    void testNested() {
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
                assertEquals("T2", where);
            }

            @Override
            public void fly(String what) {
                assertEquals("plane", what);
            }

            @Override
            public Endpoint destination(String target) {
                return result -> {
                    switch (target) {
                        case "Chile":
                            assertEquals("Santiago", result);
                            break;
                        case "Peru":
                            assertEquals("Lima", result);
                            break;
                        default:
                            fail();
                    }
                };
            }
        });

        methodReader.readOne();
        methodReader.readOne();
        methodReader.readOne();
        methodReader.readOne();
    }

    /**
     * Interface resembling QWG's Transport.
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
