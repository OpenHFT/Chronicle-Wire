/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * A method reader hands the same DTO instance to the listener for every message, so whatever one message
 * leaves behind is what the next read starts from. The DTO here reads with overwrite disabled, as in
 * {@link MarshallableWithOverwriteFalseTest}.
 *
 * @see ContainerOverwriteContractTest
 */
public class NullCollectionReusedByMethodReaderTest extends WireTestCommon {

    @Test
    public void nullThenPopulatedThenNull() {
        for (WireType wireType : new WireType[]{WireType.TEXT, WireType.BINARY_LIGHT}) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            try {
                Wire wire = wireType.apply(bytes);
                OrderListener writer = wire.methodWriter(OrderListener.class);
                writer.order(order("first", null, null, null));
                writer.order(order("second", "urgent", "call back", 5));
                writer.order(order("third", null, null, null));

                List<String> received = new ArrayList<>();
                Set<Order> instances = Collections.newSetFromMap(new IdentityHashMap<Order, Boolean>());
                MethodReader reader = wire.methodReader((OrderListener) o -> {
                    received.add(o.id + " tags=" + o.tags + " notes=" + o.notes + " limits=" + o.limits);
                    instances.add(o);
                });
                for (int i = 0; i < 3; i++)
                    assertTrue("expected message " + i, reader.readOne());
                assertFalse("only the three written messages may be read", reader.readOne());

                // an explicit null restores the declared default: null for tags, empty for notes and limits
                assertEquals(wireType.toString(), Arrays.asList(
                        "first tags=null notes=[] limits={}",
                        "second tags=[urgent] notes=[call back] limits={max=5}",
                        "third tags=null notes=[] limits={}"), received);
                assertEquals("the reader is expected to reuse the DTO", 1, instances.size());
            } finally {
                bytes.releaseLast();
            }
        }
    }

    private static Order order(String id, String tag, String note, Integer max) {
        Order order = new Order();
        order.id = id;
        order.tags = tag == null ? null : new LinkedHashSet<>(Collections.singletonList(tag));
        order.notes = note == null ? null : new ArrayList<>(Collections.singletonList(note));
        order.limits = max == null ? null : new LinkedHashMap<>(Collections.singletonMap("max", max));
        return order;
    }

    interface OrderListener {
        void order(Order order);
    }

    static class Order extends SelfDescribingMarshallable {
        String id;
        Set<String> tags;
        List<String> notes = new ArrayList<>();
        Map<String, Integer> limits = new LinkedHashMap<>();

        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            Wires.readMarshallable(this, wire, false);
        }
    }
}
