/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class Issue751Test extends WireTestCommon {

    public static class One extends SelfDescribingMarshallable {
        final Comparable<?> text;

        One(Comparable<?> text) {
            this.text = text;
        }
    }

    public static class Two implements Comparable<Two>, Marshallable {
        final Comparable<?> text;

        Two(Comparable<?> text) {
            this.text = text;
        }

        @Override
        public int compareTo(@NotNull Issue751Test.Two o) {
            return text.hashCode() - o.text.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Two))
                return false;
            Two that = (Two) o;
            return text.equals(that.text);
        }

        @Override
        public int hashCode() {
            return text.hashCode();
        }
    }

    static class Three extends SelfDescribingMarshallable {
        final One one;
        final Two two;

        Three(One one, Two two) {
            this.one = one;
            this.two = two;
        }
    }

    @Test
    @DisplayName("Comparable fields should serialise to YAML")
    public void comparableField() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for comparable field test");

        Wire wire = new YamlWire();
        wire.write("first").object(new Three(
                new One("hello"), new Two(42)));

        Object first = wire.read("first").object();
        assertEquals("!net.openhft.chronicle.wire.issue.Issue751Test$Three {\n" +
                "  one: {\n" +
                "    text: hello\n" +
                "  },\n" +
                "  two: {\n" +
                "    text: !int 42\n" +
                "  }\n" +
                "}\n", first.toString(),
                "Comparable fields should render expected YAML output");
        Three parsed = (Three) first;
        assertEquals("hello", parsed.one.text,
                "One.text should round-trip for comparable field test");
        assertEquals(42, parsed.two.text,
                "Two.text should round-trip for comparable field test");
    }
}
