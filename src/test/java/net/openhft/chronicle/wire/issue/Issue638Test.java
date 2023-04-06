package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;

import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class Issue638Test {
    @Test
    public void readJsonYamlInterleaved() {
        assertEquals(new OneField("2050-01-01T20:23:41.628447273"),
                WireType.JSON.fromString(OneField.class, "{a: \"2050-01-01T20:23:41.628447273\"}"));
        assertEquals(new OneField("2050-01-01T20:23:41.628447273"),
                WireType.TEXT.fromString(OneField.class, "a: 2050-01-01T20:23:41.628447273"));
        assertEquals(new OneField("2050-01-01T20:23:41.628447273"),
                WireType.JSON.fromString(OneField.class, "{a: \"2050-01-01T20:23:41.628447273\"}"));
        assertEquals(new OneField("2050-01-01T20:23:41.628447273"),
                WireType.TEXT.fromString(OneField.class, "a: 2050-01-01T20:23:41.628447273"));
    }

    public static class OneField implements Marshallable {
        private Bytes a;

        public OneField(String a) {
            this.a = Bytes.from(a);
        }

        @Override
        public String toString() {
            return "OneField{a=" + a + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OneField oneField = (OneField) o;
            return a.contentEquals(((OneField)o).a);
        }

        @Override
        public int hashCode() {
            return Objects.hash(a);
        }
    }
}
