/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.bytesmarshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;

import java.util.Objects;

import static org.junit.Assert.*;

public class NestedGenericTest extends WireTestCommon {
    @Test
    public void testGeneric() {
        ignoreException("BytesMarshallable found in field which is not matching exactly, the object may not unmarshall correctly if that type is not specified: " +
                "net.openhft.chronicle.wire.bytesmarshallable.NestedGenericTest$A. The warning will not repeat so there may be more types affected.");
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire wire = WireType.BINARY.apply(bytes);

        ValueHolder<A> object = new ValueHolder<>(new A(2, "b"));
        wire.getValueOut().object(object);

        assertFalse(wire.bytes().writePosition() > 150);
        assertNotEquals(object, wire.getValueIn().object());
    }

    @Test
    public void testDefined() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
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

        @Override
        public int hashCode() {
            throw new UnsupportedOperationException();
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

        @Override
        public int hashCode() {
            throw new UnsupportedOperationException();
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

        @Override
        public int hashCode() {
            throw new UnsupportedOperationException();
        }
    }
}
