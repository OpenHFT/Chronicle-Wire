/*
 * Copyright 2016-2020 chronicle.software
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
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * This class tests the capability of the MethodReader to handle parametrized interfaces hierarchy and chained calls.
 * This pattern arises when using Web Gateway.
 */
public class MethodReaderChainedInterceptedGenericInterfaceTest extends WireTestCommon {
    @SuppressWarnings("deprecation")
    @Test
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
     * Interface which extends Transport but does not clarify its method.
     */
    interface IndefiniteIface extends Transport<Endpoint> {
        void fly(String what);
    }

    /**
     * Interface which extends Transport and clarifies its method.
     */
    interface DefinitiveIface extends Transport<Endpoint> {
        void ride(String what);

        @Override
        Endpoint destination(String target);
    }
}
