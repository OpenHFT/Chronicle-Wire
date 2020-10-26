/*
 * Copyright 2016-2020 chronicle.software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.ReadResolvable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

public class EnumTest extends WireTestCommon {

    @Test
    public void testEnum() {
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        try {
            @NotNull TextWire wire = new TextWire(bytes);
            wire.write("test")
                    .object(TestEnum.INSTANCE);
            assertEquals("test: !net.openhft.chronicle.wire.EnumTest$TestEnum INSTANCE\n", wire.toString());
            @NotNull TextWire wire2 = TextWire.from(
                    "test: !net.openhft.chronicle.wire.EnumTest$TestEnum {\n" +
                            "}\n");
            @Nullable Object enumObject = wire2.read(() -> "test")
                    .object();
            Assert.assertSame(enumObject, TestEnum.INSTANCE);
        } finally {
            bytes.releaseLast();
        }
    }

    public enum TestEnum implements Marshallable, ReadResolvable<TestEnum> {
        INSTANCE;

        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        }

        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
        }

        @NotNull
        @Override
        public EnumTest.TestEnum readResolve() {
            return INSTANCE;
        }
    }
}