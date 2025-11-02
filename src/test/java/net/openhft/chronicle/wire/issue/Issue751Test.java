/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assume.assumeFalse;

public class Issue751Test extends WireTestCommon {

    public static class One extends SelfDescribingMarshallable {
        Comparable<?> text;

        public One(Comparable<?> text) {
            this.text = text;
        }
    }

    public static class Two implements Comparable<Two>, Marshallable {
        Comparable<?> text;

        public Two(Comparable<?> text) {
            this.text = text;
        }

        @Override
        public int compareTo(@NotNull Issue751Test.Two o) {
            return text.hashCode() - o.text.hashCode();
        }
    }

    public static class Three extends SelfDescribingMarshallable {
        public One one;
        public Two two;

        public Three(One one, Two two) {
            this.one = one;
            this.two = two;
        }
    }

    @Test
    public void comparableField() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        Wire wire = new YamlWire();
        wire.write("first").object(new Three(
                new One("hello"), new Two(42)));

        System.err.println(wire.read("first").object());
    }
}
