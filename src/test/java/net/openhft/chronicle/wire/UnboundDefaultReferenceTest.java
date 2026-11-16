/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongValue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class UnboundDefaultReferenceTest extends WireTestCommon {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> types() {
        return Arrays.asList(new Object[][]{{WireType.BINARY}, {WireType.TEXT}});
    }
    private final WireType type;
    public UnboundDefaultReferenceTest(WireType type) { this.type = type; }

    @Test
    public void bindingDefaultInitialisesNewReference() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = type.apply(bytes);
        LongValue initial = wire.newLongReference();
        try {
            wire.write("present").int32(1);
            assertSame(initial, wire.read("missing").int64ForBinding(initial));
            assertEquals(0, initial.getValue());
        } finally { initial.close(); bytes.releaseLast(); }
    }
    @Test
    public void unboundLongSetterReferenceIsInitialised() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = type.apply(bytes);
        try (LongValue initial = wire.newLongReference()) {
            wire.write("present").int32(1);
            AtomicReference<LongValue> result = new AtomicReference<>();
            wire.read("missing").int64(initial, result, AtomicReference::set);
            assertSame(initial, result.get());
            assertEquals(0, result.get().getValue());
        } finally { bytes.releaseLast(); }
    }
    @Test
    public void unboundIntSetterReferenceIsInitialised() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = type.apply(bytes);
        try (IntValue initial = wire.newIntReference()) {
            wire.write("present").int32(1);
            AtomicReference<IntValue> result = new AtomicReference<>();
            wire.read("missing").int32(initial, result, AtomicReference::set);
            assertSame(initial, result.get());
            assertEquals(0, result.get().getValue());
        } finally { bytes.releaseLast(); }
    }
}
