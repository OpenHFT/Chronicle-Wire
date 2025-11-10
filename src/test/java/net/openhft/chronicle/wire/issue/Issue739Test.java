//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.*;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

/**
 * see https://github.com/OpenHFT/Chronicle-Wire/issues/739
 */
public class Issue739Test extends WireTestCommon {

    static class One extends SelfDescribingMarshallable {
        String text;

        public One(String text) {
            this.text = text;
        }
    }

    static class Two extends SelfDescribingMarshallable {
        String text;

        public Two(String text) {
            this.text = text;
        }
    }

    static class Three extends SelfDescribingMarshallable {
        private One one;
        private Two two;
        private One twoAndHalf;
    }

    static class IThree extends SelfDescribingMarshallable {
        private Marshallable one;
        private Marshallable two;
        private Marshallable twoAndHalf;

    }

    @Test
    public void fieldAnchorAlias() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Wire wire = new YamlWire(Bytes.wrapForRead(("three: !net.openhft.chronicle.wire.issue.Issue739Test$Three\n" +
                "  one: &first\n" +
                "    text: hello\n" +
                "  two:\n" +
                "    text: world\n" +
                "  twoAndHalf: *first\n").getBytes()));
        Three three = (Three) wire.getValueIn().<Map<String, Object>>typedMarshallable().get("three");
        assertEquals("hello", three.one.text);
        assertEquals("world", three.two.text);
        assertEquals("hello", three.twoAndHalf.text);
        assertSame(three.one, three.twoAndHalf);
    }

    @Test
    public void interfaceFieldAnchorAlias() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Wire wire = new YamlWire(Bytes.wrapForRead(("three: !net.openhft.chronicle.wire.issue.Issue739Test$IThree\n" +
                "  one: &first !net.openhft.chronicle.wire.issue.Issue739Test$One\n" +
                "    text: hello\n" +
                "  two: !net.openhft.chronicle.wire.issue.Issue739Test$Two\n" +
                "    text: world\n" +
                "  twoAndHalf: *first\n").getBytes()));
        IThree three = (IThree) wire.getValueIn().<Map<String, Object>>typedMarshallable().get("three");
        assertEquals("hello", ((One)three.one).text);
        assertEquals("world", ((Two)three.two).text);
        assertEquals("hello", ((One)three.twoAndHalf).text);
        assertSame(three.one, three.twoAndHalf);
    }

    @Test
    public void anchorOfTextField() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Wire wire = new YamlWire(Bytes.wrapForRead(("three: !net.openhft.chronicle.wire.issue.Issue739Test$IThree\n" +
                "  one: &first !net.openhft.chronicle.wire.issue.Issue739Test$One\n" +
                "    text: &msg hello\n" +
                "  two: !net.openhft.chronicle.wire.issue.Issue739Test$Two\n" +
                "    text: *msg\n" +
                "  twoAndHalf: !net.openhft.chronicle.wire.issue.Issue739Test$One\n" +
                "    text: world\n").getBytes()));
        IThree three = (IThree) wire.getValueIn().<Map<String, Object>>typedMarshallable().get("three");
        assertEquals("hello", ((One)three.one).text);
        assertEquals("hello", ((Two)three.two).text);
        assertEquals("world", ((One)three.twoAndHalf).text);
    }
}
