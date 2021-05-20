/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static net.openhft.chronicle.bytes.Bytes.elasticByteBuffer;
import static net.openhft.chronicle.wire.WireType.BINARY;

public class UsingTestMarshallable {

    @Test
    public void testConverMarshallableToTextName() {

        @NotNull TestMarshallable testMarshallable = new TestMarshallable();
        testMarshallable.setName("hello world");

        Bytes<ByteBuffer> byteBufferBytes = Bytes.elasticByteBuffer();

        @NotNull Wire wire = new TextWire(byteBufferBytes);
        wire.usePadding(true);
        wire.bytes().readPosition();

        wire.writeDocument(false, d -> d.write(() -> "any-key").marshallable(testMarshallable));

        String value = Wires.fromSizePrefixedBlobs(wire.bytes());

        //String replace = value.replace("\n", "\\n");

       // System.out.println(byteBufferBytes.toHexString());
       // System.out.println(value);

         // Assert.assertTrue(replace.length() > 1);
        byteBufferBytes.releaseLast();
    }

    /**
     * see WIRE-37 issue when using numbers as keys in binary wire
     */
    @Test
    public void testMarshall() {

        @SuppressWarnings("rawtypes")
        Bytes bytes = Bytes.elasticByteBuffer();
        @NotNull Wire wire = new BinaryWire(bytes);

        @NotNull MyMarshallable x = new MyMarshallable();
        x.text.append("text");

        wire.write(() -> "key").typedMarshallable(x);

        @NotNull final ValueIn read = wire.read(() -> "key");
        @Nullable final MyMarshallable result = read.typedMarshallable();

       // System.out.println(result.toString());

        Assert.assertEquals("text", result.text.toString());

        bytes.releaseLast();
    }

    @Test
    public void test() {

        Wire wire = WireType.BINARY.apply(Wires.acquireBytes());
        @NotNull MarshableFilter expected = new MarshableFilter("hello", "world");

        // write
        {
            @NotNull SortedFilter sortedFilter = new SortedFilter();

            boolean add = sortedFilter.marshableFilters.add(expected);
            Assert.assertTrue(add);
            wire.write().marshallable(sortedFilter);
        }

        // read
        {
            @NotNull SortedFilter sortedFilter = new SortedFilter();
            wire.read().marshallable(sortedFilter);
            Assert.assertEquals(1, sortedFilter.marshableFilters.size());
            Assert.assertEquals(expected, sortedFilter.marshableFilters.get(0));
        }
    }

    public static class MyMarshallable implements Marshallable {

        @NotNull
        public StringBuilder text = new StringBuilder();

        @Override
        public void readMarshallable(@NotNull WireIn wire) {
            wire.read(() -> "262").text(text);
        }

        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            wire.write(() -> "262").text(text);
        }

        @NotNull
        @Override
        public String toString() {
            return "X{" +
                    "text=" + text +
                    '}';
        }
    }

    static class MarshableFilter extends SelfDescribingMarshallable {
        @NotNull
        public final String columnName;
        @NotNull
        public final String filter;

        public MarshableFilter(@NotNull String columnName, @NotNull String filter) {
            this.columnName = columnName;
            this.filter = filter;
        }
    }

    static class MarshableOrderBy extends SelfDescribingMarshallable {
        @NotNull
        public final String column;
        public final boolean isAscending;

        public MarshableOrderBy(@NotNull String column, boolean isAscending) {
            this.column = column;
            this.isAscending = isAscending;
        }
    }

    static class SortedFilter extends SelfDescribingMarshallable {
        public long fromIndex;
        @NotNull
        public List<MarshableOrderBy> marshableOrderBy = new ArrayList<>();
        @NotNull
        public List<MarshableFilter> marshableFilters = new ArrayList<>();
    }
}
