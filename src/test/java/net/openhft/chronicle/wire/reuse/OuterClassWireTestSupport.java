/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;

import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

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
        assertEquals("test1", sb.toString());
        assertEquals(normaliseNewlines ? first.toString().replace(',', '\n') : first.toString(),
                normaliseNewlines ? target.toString().replace(',', '\n') : target.toString());

        wire.readEventName(sb).marshallable(target);
        assertEquals("test2", sb.toString());
        assertEquals(normaliseNewlines ? second.toString().replace(',', '\n') : second.toString(),
                normaliseNewlines ? target.toString().replace(',', '\n') : target.toString());

        bytes.releaseLast();
    }
}
