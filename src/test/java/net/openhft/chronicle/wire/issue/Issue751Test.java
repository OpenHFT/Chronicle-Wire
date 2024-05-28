package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

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
        private One one;
        private Two two;

        public Three(One one, Two two) {
            this.one = one;
            this.two = two;
        }
    }

    @Test
    public void comparableField() {
        Wire wire = new YamlWire(Bytes.allocateElasticOnHeap());
        wire.write("first").object(new Three(
                new One("hello"), new Two(42)));

        System.err.println(wire.read("first").object());
    }
}
