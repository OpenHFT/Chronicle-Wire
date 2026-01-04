/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class OuterClassWireTestSupport {
    private OuterClassWireTestSupport() {
    }

    public static <T extends AbstractPooledOuterClass<?>> void assertTwoOuterClasses(Function<Bytes<?>, Wire> wireType,
                                                                                     Supplier<T> supplier,
                                                                                     T first,
                                                                                     T second,
                                                                                     boolean normaliseNewlines) {
        Bytes<?> bytes = Bytes.elasticByteBuffer();
        Wire wire = wireType.apply(bytes);

        wire.writeEventName(() -> "test1").marshallable(first);
        if (wireType == WireType.JSON)
            wire.bytes().writeUnsignedByte('\n');
        wire.writeEventName(() -> "test2").marshallable(second);

        StringBuilder sb = new StringBuilder();
        T target = supplier.get();

        wire.readEventName(sb).marshallable(target);
        assertEquals("test1", sb.toString(), "First event name should be test1");
        assertEquals(normaliseNewlines ? normalise(first) : first.toString(),
                normaliseNewlines ? normalise(target) : target.toString(),
                "First outer class should round-trip via marshalling");

        wire.readEventName(sb).marshallable(target);
        assertEquals("test2", sb.toString(), "Second event name should be test2");
        assertEquals(normaliseNewlines ? normalise(second) : second.toString(),
                normaliseNewlines ? normalise(target) : target.toString(),
                "Second outer class should round-trip via marshalling");

        bytes.releaseLast();
    }

    private static <T> @NotNull String normalise(T first) {
        return first.toString().replace(',', '\n');
    }
}
