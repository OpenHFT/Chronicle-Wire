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
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * Created by Rob Austin
 */
public class UsingTestMarshallable {

    @Test
    public void testConverMarshallableToTextName() {

        TestMarshallable testMarshallable = new TestMarshallable();
        testMarshallable.setName("hello world");

        Bytes<ByteBuffer> byteBufferBytes = Bytes.elasticByteBuffer();

        ByteBuffer byteBuffer = byteBufferBytes.underlyingObject();
        System.out.println(byteBuffer.getClass());

        Wire textWire = new TextWire(byteBufferBytes);
        textWire.bytes().readPosition();

        textWire.writeDocument(false, d -> d.write(() -> "any-key").marshallable(testMarshallable));

        String value = Wires.fromSizePrefixedBlobs(textWire.bytes());

        //String replace = value.replace("\n", "\\n");

        System.out.println(byteBufferBytes.toHexString());
        System.out.println(value);

        //  Assert.assertTrue(replace.length() > 1);
    }

    /**
     * see WIRE-37 issue when using numbers as keys in binary wire
     */
    @Test
    public void testMarshall() {

        Bytes bytes = Bytes.elasticByteBuffer();
        Wire wire = new BinaryWire(bytes);

        MyMarshallable x = new MyMarshallable();
        x.text.append("text");

        wire.write(() -> "key").typedMarshallable(x);

        final ValueIn read = wire.read(() -> "key");
        final MyMarshallable result = read.typedMarshallable();

        System.out.println(result.toString());

        Assert.assertEquals("text", result.text.toString());
    }

    public static class MyMarshallable implements Marshallable {

        public StringBuilder text = new StringBuilder();

        @Override
        public void readMarshallable(@NotNull WireIn wire) {
            wire.read(() -> "262").text(text);
        }

        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            wire.write(() -> "262").text(text);
        }

        @Override
        public String toString() {
            return "X{" +
                    "text=" + text +
                    '}';
        }
    }
}
