/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ShortTextLongConverterTest extends WireTestCommon {

    private static final CharSequence TEST_STRING = "world";

    @Test
    void parseLeadingZero() {
        LongConverter c = ShortTextLongConverter.INSTANCE;
        assertEquals(0L, c.parse(""));
        assertEquals(0L, c.parse(" "));
        assertEquals(0L, c.parse("  "));
        assertEquals(84L, c.parse("0"));
        assertEquals(84L, c.parse(" 0"));
        assertEquals(7224, c.parse("00"));
        assertEquals(7224, c.parse("  00"));
        assertEquals(614124, c.parse("000"));
        assertEquals(52200624, c.parse("0000"));
        assertEquals(4437053124L, c.parse("00000"));
        assertEquals(377149515624L, c.parse("000000"));
        assertEquals(32057708828124L, c.parse("0000000"));
        assertEquals(2724905250390624L, c.parse("00000000"));
        assertEquals(231616946283203124L, c.parse("000000000"));
        assertEquals(1240696360362714008L, c.parse("0000000000"));
        assertThrows(IllegalArgumentException.class, () -> c.parse("00000000000"));
        assertEquals("", c.asString(0L));
    }

    @Test
    void parse() {
        LongConverter c = ShortTextLongConverter.INSTANCE;
        // System.out.println(c.asString(-1L));
        for (String s : ",a,ab,abc,abcd,ab.de,123=56,1234567,12345678,zzzzzzzzz,+ko2&)z.0".split(",")) {
            long v = c.parse(s);
            StringBuilder sb = new StringBuilder();
            c.append(sb, v);
            assertEquals(s, sb.toString());
        }
    }

    @Test
    void parseSubsequence() {
        LongConverter c = ShortTextLongConverter.INSTANCE;
        String s = ",a,ab,abc,abcd,ab.de,123=56,1234567,12345678,zzzzzzzzz,+ko2&)z.0,";
        int comparisons = 11;
        subStringParseLoop(s, c, comparisons);
        assertTrue(true);
    }

    @Test
    void parseLengthCheck() {
        assertThrows(IllegalArgumentException.class, () -> ShortTextLongConverter.INSTANCE.parse(getClass().getCanonicalName()));
    }

    @Test
    void parseSubstringLengthCheck() {
        assertThrows(IllegalArgumentException.class, () -> ShortTextLongConverter.INSTANCE.parse("abcd", 3, -2));
    }

    @Test
    void asString() {
        LongConverter c = ShortTextLongConverter.INSTANCE;
        IntStream.range(0, 10_000_000)
                .parallel()
                .mapToLong(i -> ThreadLocalRandom.current().nextLong())
                .forEach(l -> {
                    String s = c.asString(l);
                    assertEquals(l, c.parse(s), s);
                });
    }

    @Test
    void testAppend() {
        final Bytes<?> b = Bytes.allocateElasticOnHeap();
        try {
            final LongConverter idLongConverter = ShortTextLongConverter.INSTANCE;
            final long helloWorld = idLongConverter.parse(TEST_STRING);
            idLongConverter.append(b, helloWorld);
            assertEquals(TEST_STRING, b.toString());
        } finally {
            b.releaseLast();
        }
    }

    @Test
    void testAppendWithExistingData() {
        final Bytes<?> b = Bytes.allocateElasticOnHeap().append("hello");
        try {
            final LongConverter idLongConverter = ShortTextLongConverter.INSTANCE;
            final long helloWorld = idLongConverter.parse(TEST_STRING);
            idLongConverter.append(b, helloWorld);
            assertEquals("hello" + TEST_STRING, b.toString());
        } finally {
            b.releaseLast();
        }
    }

    @Test
    void allSafeCharsTextWire() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        allSafeChars(wire);
    }

    @Test
    void allSafeCharsYamlWire() {
        Wire wire = new YamlWire();
        allSafeChars(wire);
    }

    private void allSafeChars(Wire wire) {
        final LongConverter converter = ShortTextLongConverter.INSTANCE;
        for (long i = 0; i <= 85 * 85; i++) {
            wire.clear();
            wire.write("a").writeLong(converter, i);
            wire.write("b").sequence(i, (i2, v) -> {
                v.writeLong(converter, i2);
                v.writeLong(converter, i2);
            });
            assertEquals(i, wire.read("a").readLong(converter), wire.toString());
            wire.read("b").sequence(i, (i2, v) -> {
                assertEquals((long) i2, v.readLong(converter));
                assertEquals((long) i2, v.readLong(converter));
            });
        }
    }
}
