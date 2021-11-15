package net.openhft.chronicle.wire.bytesmarshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.wire.*;
import org.junit.Test;

import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NestedGenericTest {
    @Test
    public void testGeneric() {
        Bytes bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.BINARY.apply(bytes);

        ValueHolder<A> object = new ValueHolder<>(new A(2, "b"));
        wire.getValueOut().object(object);

        assertTrue(wire.bytes().writePosition() > 150);
        assertEquals(object, wire.getValueIn().object());
    }

    @Test
    public void testDefined() {
        Bytes bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.BINARY.apply(bytes);

        ValueHolderDef object = new ValueHolderDef(new A(2, "b"));
        wire.getValueOut().object(object);

        assertTrue(wire.bytes().writePosition() < 150);
        assertEquals(object, wire.getValueIn().object());
    }

    private static class ValueHolder<V> {
        private final V defaultValue;

        public ValueHolder(V defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ValueHolder<?> that = (ValueHolder<?>) o;
            return Objects.equals(defaultValue, that.defaultValue);
        }
    }

    private static class ValueHolderDef {
        private final A defaultValue;

        public ValueHolderDef(A defaultValue) {
            this.defaultValue = defaultValue;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ValueHolderDef that = (ValueHolderDef) o;
            return Objects.equals(defaultValue, that.defaultValue);
        }
    }

    private static class A implements BytesMarshallable {
        int x;
        String y;

        public A(int x, String y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            A a = (A) o;
            return x == a.x && Objects.equals(y, a.y);
        }
    }
}
