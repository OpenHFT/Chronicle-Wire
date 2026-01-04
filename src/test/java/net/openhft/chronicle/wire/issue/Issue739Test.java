/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.issue;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Issue 739: YAML anchors should resolve to shared instances across repeated fields.
 */
class Issue739Test extends WireTestCommon {

    static class One extends SelfDescribingMarshallable {
        final String text;

        public One(String text) {
            this.text = text;
        }
    }

    static class Two extends SelfDescribingMarshallable {
        final String text;

        public Two(String text) {
            this.text = text;
        }
    }

    @SuppressFBWarnings(value = "UWF_UNWRITTEN_FIELD", justification = "Fields populated by wire marshalling")
    static class Three extends SelfDescribingMarshallable {
        private One one;
        private Two two;
        private One twoAndHalf;
    }

    @SuppressFBWarnings(value = "UWF_UNWRITTEN_FIELD", justification = "Fields populated by wire marshalling")
    static class IThree extends SelfDescribingMarshallable {
        private Marshallable one;
        private Marshallable two;
        private Marshallable twoAndHalf;

    }

    @Test
    @DisplayName("Resolves anchor aliases for concrete field types")
    void fieldAnchorAlias() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory disabled; skip concrete field anchor alias test");

        Wire wire = new YamlWire(Bytes.wrapForRead(("three: !net.openhft.chronicle.wire.issue.Issue739Test$Three\n" +
                "  one: &first\n" +
                "    text: hello\n" +
                "  two:\n" +
                "    text: world\n" +
                "  twoAndHalf: *first\n").getBytes(StandardCharsets.UTF_8)));
        Three three = (Three) wire.getValueIn().<Map<String, Object>>typedMarshallable().get("three");
        assertEquals("hello", three.one.text, "Anchor alias should populate one.text");
        assertEquals("world", three.two.text, "Anchor alias should populate two.text");
        assertEquals("hello", three.twoAndHalf.text, "Anchor alias should populate twoAndHalf.text from *first");
        assertSame(three.one, three.twoAndHalf, "Anchor alias should reuse the one instance for twoAndHalf");
    }

    @Test
    @DisplayName("Resolves anchor aliases for interface-typed fields")
    void interfaceFieldAnchorAlias() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory disabled; skip interface field anchor alias test");

        Wire wire = new YamlWire(Bytes.wrapForRead(("three: !net.openhft.chronicle.wire.issue.Issue739Test$IThree\n" +
                "  one: &first !net.openhft.chronicle.wire.issue.Issue739Test$One\n" +
                "    text: hello\n" +
                "  two: !net.openhft.chronicle.wire.issue.Issue739Test$Two\n" +
                "    text: world\n" +
                "  twoAndHalf: *first\n").getBytes(StandardCharsets.UTF_8)));
        IThree three = (IThree) wire.getValueIn().<Map<String, Object>>typedMarshallable().get("three");
        assertEquals("hello", ((One) three.one).text, "Anchor alias should populate one.text via interface");
        assertEquals("world", ((Two) three.two).text, "Anchor alias should populate two.text via interface");
        assertEquals("hello", ((One) three.twoAndHalf).text, "Anchor alias should populate twoAndHalf.text via interface");
        assertSame(three.one, three.twoAndHalf, "Anchor alias should reuse the one instance for interface field");
    }

    @Test
    @DisplayName("Reuses anchors when text fields share content")
    void anchorOfTextField() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory disabled; skip text anchor alias test");

        Wire wire = new YamlWire(Bytes.wrapForRead(("three: !net.openhft.chronicle.wire.issue.Issue739Test$IThree\n" +
                "  one: &first !net.openhft.chronicle.wire.issue.Issue739Test$One\n" +
                "    text: &msg hello\n" +
                "  two: !net.openhft.chronicle.wire.issue.Issue739Test$Two\n" +
                "    text: *msg\n" +
                "  twoAndHalf: !net.openhft.chronicle.wire.issue.Issue739Test$One\n" +
                "    text: world\n").getBytes(StandardCharsets.UTF_8)));
        IThree three = (IThree) wire.getValueIn().<Map<String, Object>>typedMarshallable().get("three");
        assertEquals("hello", ((One) three.one).text, "Text anchor should populate one.text");
        assertEquals("hello", ((Two) three.two).text, "Text anchor should populate two.text");
        assertEquals("world", ((One) three.twoAndHalf).text, "Unanchored text should populate twoAndHalf.text");
    }
}
