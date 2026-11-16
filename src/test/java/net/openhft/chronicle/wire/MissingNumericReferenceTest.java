/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.Closeable;
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
public class MissingNumericReferenceTest extends WireTestCommon {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> types() {
        return Arrays.asList(new Object[][]{{WireType.BINARY}, {WireType.TEXT}});
    }

    private final WireType type;

    public MissingNumericReferenceTest(WireType type) {
        this.type = type;
    }

    @Test
    public void missingIntCreatesWritableZeroReference() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        AtomicReference<IntValue> target = new AtomicReference<>();
        try {
            Wire wire = type.apply(bytes);
            wire.write("present").int32(1);
            wire.read("absent").int32(null, target, AtomicReference::set);
            assertNotNull(target.get());
            assertEquals(0, target.get().getValue());
            target.get().setValue(42);
            assertEquals(42, target.get().getValue());
            IntValue existing = target.get();
            DefaultValueIn defaults = new DefaultValueIn(wire);
            defaults.defaultValue = 17;
            defaults.int32(existing, target, AtomicReference::set);
            assertSame(existing, target.get());
            assertEquals(17, target.get().getValue());
        } finally {
            Closeable.closeQuietly(target.get());
            bytes.releaseLast();
        }
    }

    @Test
    public void missingLongCreatesWritableZeroReference() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        AtomicReference<LongValue> target = new AtomicReference<>();
        try {
            Wire wire = type.apply(bytes);
            wire.write("present").int32(1);
            wire.read("absent").int64(null, target, AtomicReference::set);
            assertNotNull(target.get());
            assertEquals(0, target.get().getValue());
            target.get().setValue(1L << 40);
            assertEquals(1L << 40, target.get().getValue());
            LongValue existing = target.get();
            DefaultValueIn defaults = new DefaultValueIn(wire);
            defaults.defaultValue = 1L << 42;
            defaults.int64(existing, target, AtomicReference::set);
            assertSame(existing, target.get());
            assertEquals(1L << 42, target.get().getValue());
        } finally {
            Closeable.closeQuietly(target.get());
            bytes.releaseLast();
        }
    }

    @Test
    public void rejectedDefaultReferenceIsClosed() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        AtomicReference<IntValue> target = new AtomicReference<>();
        try {
            Wire wire = type.apply(bytes);
            wire.write("present").int32(1);
            ValueIn missingField = wire.read("absent");
            IllegalStateException failure = new IllegalStateException("rejected");
            assertSame(failure, assertThrows(IllegalStateException.class, () ->
                    missingField.int32(null, target, (holder, value) -> {
                        holder.set(value);
                        throw failure;
                    })));
            assertTrue(target.get().isClosed());
        } finally {
            Closeable.closeQuietly(target.get());
            bytes.releaseLast();
        }
    }
}
