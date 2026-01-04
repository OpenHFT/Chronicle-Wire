/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.JSONWire;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Regression coverage for JSON type aliasing metadata behaviour in issue 322.
 */
public class JSON322Test extends WireTestCommon {

    @Test
    @DisplayName("JSON wire should support nested types")
    public void supportNestedTypes() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for nested JSON type test");

        final Three three = new Three();
        three.one = new One("hello");
        three.two = new Four("world");

        final Bytes<?> bytes = Bytes.allocateElasticOnHeap();

        JSONWire wire = new JSONWire(bytes)
                .useTypes(true);
        wire.getValueOut()
                .object(three);

        final String expected = "{\"@net.openhft.chronicle.wire.issue.JSON322Test$Three\":{\"one\":{\"@net.openhft.chronicle.wire.issue.JSON322Test$One\":{\"text\":\"hello\"}},\"two\":{\"@net.openhft.chronicle.wire.issue.JSON322Test$Four\":{\"text\":\"world\"}}}}";
        final String actual = wire.bytes().toString();
        assertEquals(expected, actual, "Wire output should include nested type metadata");

        // Now try reading it back again
        final JSONWire parserWire = new JSONWire(bytes)
                .useTypes(true);

        final Object parsed = parserWire.getValueIn().object();

        assertNotNull(parsed, "Nested type parse result should not be null");
        assertEquals(Three.class, parsed.getClass(), "Parsed object should be Three");

        final Three parsedThree = (Three) parsed;

        assertEquals(One.class, parsedThree.one.getClass(), "Nested type one should be One");
        assertEquals(Four.class, parsedThree.two.getClass(), "Nested type two should be Four");
        assertEquals("hello", parsedThree.one.text, "Nested type one text should match");
        Two parsedTwo = parsedThree.two;
        assertEquals("world", parsedTwo.text, "Nested type two base text should match");
        Four parsedFour = (Four) parsedTwo;
        assertEquals("world", parsedFour.text, "Nested type four text should match");
        assertEquals(three, parsed, "Nested type parse result should equal original Three");
    }

    @Test
    @DisplayName("JSON wire should support alias types")
    public void supportTypes() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for alias JSON type test");

        ClassAliasPool.CLASS_ALIASES.addAlias(Combined322.class, TypeOne322.class, TypeTwo322.class);
        Combined322 c = new Combined322();
        List<SelfDescribingMarshallable> list = c.list = new ArrayList<>();
        list.add(new TypeOne322("one"));
        list.add(new TypeTwo322(2, 22));
        c.t1 = new TypeOne322("one-one");
        c.t2 = new TypeTwo322(222, 2020);

        final Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        JSONWire wire = new JSONWire(bytes)
                .useTypes(true);
        wire.getValueOut()
                .object(c);

        assertEquals("{\"@Combined322\":{" +
                        "\"t1\":{\"@TypeOne322\":{\"text\":\"one-one\"}}," +
                        "\"t2\":{\"@TypeTwo322\":{\"id\":222,\"value\":2020}}," +
                        "\"list\":[ {\"@TypeOne322\":{\"text\":\"one\"}},{\"@TypeTwo322\":{\"id\":2,\"value\":22}} ]}}",
                wire.bytes().toString(),
                "Wire output should include aliased type metadata");

        // Now try reading it back again
        final JSONWire parserWire = new JSONWire(bytes)
                .useTypes(true);

        final Object parsed = parserWire.getValueIn().object();

        assertNotNull(parsed, "Aliased type parse result should not be null");
        assertEquals(Combined322.class, parsed.getClass(), "Parsed object should be Combined322");

        final Combined322 combined322 = (Combined322) parsed;

        assertEquals(TypeOne322.class, combined322.t1.getClass(), "t1 should be TypeOne322");
        assertEquals(TypeTwo322.class, combined322.t2.getClass(), "t2 should be TypeTwo322");
        final List<? extends SelfDescribingMarshallable> l = combined322.list;
        assertEquals(2, l.size(), "List should contain two entries");
        assertEquals(TypeOne322.class, l.get(0).getClass(), "First list entry should be TypeOne322");
        assertEquals(TypeTwo322.class, l.get(1).getClass(), "Second list entry should be TypeTwo322");

        assertEquals(c, combined322, "Aliased type parse result should equal original Combined322");
    }

    static class One extends SelfDescribingMarshallable {
        final String text;

        One(String text) {
            this.text = text;
        }
    }

    static class Two extends SelfDescribingMarshallable {
        final String text;

        Two(String text) {
            this.text = text;
        }
    }

    static class Four extends Two {
        final String text;

        Four(String text) {
            super(text);
            this.text = text;
        }
    }

    static class Three extends SelfDescribingMarshallable {
        private One one;
        private Two two;

        Three() {
        }
    }

    static class Combined322 extends SelfDescribingMarshallable {
        TypeOne322 t1;
        TypeTwo322 t2;
        List<SelfDescribingMarshallable> list;
    }

    static class TypeOne322 extends SelfDescribingMarshallable {
        final String text;

        TypeOne322(String one) {
            text = one;
        }
    }

    static class TypeTwo322 extends SelfDescribingMarshallable {
        final int id;
        final long value;

        TypeTwo322(int id, long value) {
            this.id = id;
            this.value = value;
        }
    }
}
