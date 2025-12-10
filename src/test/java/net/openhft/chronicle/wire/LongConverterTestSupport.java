/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.LongConverter;

import static org.junit.Assert.assertEquals;

final class LongConverterTestSupport {

    private LongConverterTestSupport() {
    }

    static void assertAppend(CharSequence text, LongConverter converter) {
        final Bytes<?> b = Bytes.allocateElasticOnHeap();
        try {
            final long value = converter.parse(text);
            converter.append(b, value);
            assertEquals(text, b.toString());
        } finally {
            b.releaseLast();
        }
    }

    static void assertAppendWithPrefix(CharSequence text, LongConverter converter, String prefix) {
        final Bytes<?> b = Bytes.allocateElasticOnHeap().append(prefix);
        try {
            final long value = converter.parse(text);
            converter.append(b, value);
            assertEquals(prefix + text, b.toString());
        } finally {
            b.releaseLast();
        }
    }

    static void allSafeChars(Wire wire, LongConverter converter) {
        allSafeChars(wire, converter, 85 * 85L);
    }

    static void allSafeChars(Wire wire, LongConverter converter, long maxValue) {
        for (long i = 0; i <= maxValue; i++) {
            wire.clear();
            wire.write("a").writeLong(converter, i);
            wire.write("b").sequence(i, (i2, v) -> {
                v.writeLong(converter, i2);
                v.writeLong(converter, i2);
            });
            assertEquals(wire.toString(),
                    i, wire.read("a").readLong(converter));
            wire.read("b").sequence(i, (i2, v) -> {
                assertEquals((long) i2, v.readLong(converter));
                assertEquals((long) i2, v.readLong(converter));
            });
        }
    }
}
