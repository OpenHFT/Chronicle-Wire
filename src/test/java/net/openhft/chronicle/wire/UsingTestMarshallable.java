/*
 *
 *  *     Copyright (C) 2016  higherfrequencytrading.com
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU Lesser General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU Lesser General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU Lesser General Public License
 *  *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
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
