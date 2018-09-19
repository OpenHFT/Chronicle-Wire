/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

public class TextWireCompatibilityTest {

    @Test
    public void testAddFieldsInTheMiddle() {
        @NotNull TextWire wire = new TextWire(Bytes.elasticHeapByteBuffer(100));
        wire.getValueOut().object(new SubIncompatibleObject());
        System.out.println(wire.toString());
        Assert.assertNotNull(wire.getValueIn().object());
    }

    public static class SuperIncompatibleObject implements Marshallable {
        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            Assert.assertEquals(1, wire.read("a").int32());
            @Nullable String missingValue = wire.read("c").text();
            if (missingValue != null) {
                System.err.println("expected null, had: <" + missingValue + ">");
            }
        }

        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            wire.write(() -> "a").int32(1);
        }
    }

    public static class SubIncompatibleObject extends SuperIncompatibleObject {
        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            super.readMarshallable(wire);
            Assert.assertEquals(TextWireCompatibilityTest.class, wire.read("b").typeLiteral());
            Assert.assertNotNull(wire.read(() -> "object").object());
            Assert.assertNotNull(wire.read(() -> "object2").object());
        }

        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            super.writeMarshallable(wire);
            wire.write(() -> "b").typeLiteral(TextWireCompatibilityTest.class);
            wire.write(() -> "object").object(new SimpleObject());
            wire.write(() -> "object2").object(new SimpleObject());
        }
    }

    public static class SimpleObject implements Marshallable {
    }
}
