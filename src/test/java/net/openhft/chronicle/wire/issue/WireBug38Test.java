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

package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.nio.ByteBuffer;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;

public class WireBug38Test extends WireTestCommon {
    @Test
    public void testNestedObj() {
        @NotNull final WireType wireType = WireType.TEXT;
        @NotNull final String exampleString = "{";

        @NotNull final Outer obj1 = new Outer();
        @NotNull final Outer obj2 = new Outer();

        obj1.getObj().append(exampleString);

        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        obj1.writeMarshallable(wireType.apply(bytes));

        final String output = bytes.toString();
       // System.out.println("output: [" + output + "]");

        obj2.readMarshallable(wireType.apply(Bytes.from(output)));

        assertEquals(obj1, obj2);
        bytes.releaseLast();
    }

    static class MarshallableObj implements Marshallable {
        private final StringBuilder builder = new StringBuilder();

        public void clear() {
            builder.setLength(0);
        }

        public void append(CharSequence cs) {
            builder.append(cs);
        }

        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            builder.setLength(0);
            assertNotNull(wire.getValueIn().textTo(builder));
        }

        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            wire.getValueOut().text(builder);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            @Nullable MarshallableObj that = (MarshallableObj) o;

            return builder.toString().equals(that.builder.toString());
        }

        @Override
        public int hashCode() {
            return builder.toString().hashCode();
        }

        @NotNull
        @Override
        public String toString() {
            return builder.toString();
        }
    }

    static class Outer implements Marshallable {
        private final MarshallableObj obj = new MarshallableObj();

        @NotNull
        MarshallableObj getObj() {
            return obj;
        }

        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            wire.read(() -> "obj").marshallable(obj);
        }

        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            wire.write(() -> "obj").marshallable(obj);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            @Nullable Outer outer = (Outer) o;

            return obj.equals(outer.obj);
        }

        @Override
        public int hashCode() {
            return obj.hashCode();
        }
    }
}
