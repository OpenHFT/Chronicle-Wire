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

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

@RunWith(value = Parameterized.class)
public class SequenceTest extends WireTestCommon {

    private final WireType wireType;

    public SequenceTest(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> wireTypes() {
        Object[][] list = {
                {WireType.BINARY},
                {WireType.TEXT},
                {WireType.JSON}
        };
        return Arrays.asList(list);
    }

    @Test
    public void test() {

        My m1 = new My();
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        Wire w1 = wireType.apply(bytes);
        m1.stuff.addAll(Arrays.asList("one", "two", "three"));
        m1.writeMarshallable(w1);

        m1.stuff.clear();
        m1.stuff.addAll(Arrays.asList("four", "five", "six"));
        m1.writeMarshallable(w1);

        m1.stuff.clear();
        m1.stuff.addAll(Arrays.asList("seven", "eight"));
        m1.writeMarshallable(w1);

        {
            My m2 = new My();
            Wire w2 = wireType.apply(bytes);
            m2.readMarshallable(w2);

            assertEquals("!net.openhft.chronicle.wire.SequenceTest$My {\n" +
                    "  stuff: [\n" +
                    "    one,\n" +
                    "    two,\n" +
                    "    three\n" +
                    "  ]\n" +
                    "}\n", m2.toString());

            m2.readMarshallable(w2);

            assertEquals("!net.openhft.chronicle.wire.SequenceTest$My {\n" +
                    "  stuff: [\n" +
                    "    four,\n" +
                    "    five,\n" +
                    "    six\n" +
                    "  ]\n" +
                    "}\n", m2.toString());

            m2.readMarshallable(w2);

            assertEquals("!net.openhft.chronicle.wire.SequenceTest$My {\n" +
                    "  stuff: [\n" +
                    "    seven,\n" +
                    "    eight\n" +
                    "  ]\n" +
                    "}\n", m2.toString());
        }
        bytes.releaseLast();
    }

    @Test
    public void readSetAsObject() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire w1 = wireType.apply(bytes);
        Set<String> value = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        try (DocumentContext dc = w1.writingDocument()) {
            dc.wire().write("list").object(value);
        }
        System.out.println(Wires.fromSizePrefixedBlobs(w1));
        try (DocumentContext dc = w1.readingDocument()) {
            Object o = dc.wire().read("list").object();
            if (wireType == WireType.JSON)
                o = new LinkedHashSet<>((Collection<?>) o);
            assertEquals(value, o);
        }
    }

    @Test
    public void readListAsObject() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire w1 = wireType.apply(bytes);
        List<String> value = Arrays.asList("a", "b", "c");
        try (DocumentContext dc = w1.writingDocument()) {
            dc.wire().write("list").object(value);
        }
        System.out.println(Wires.fromSizePrefixedBlobs(w1));
        try (DocumentContext dc = w1.readingDocument()) {
            Object o = dc.wire().read("list").object();
            assertEquals(value, o);
        }
    }

    @Test
    public void readMapAsObject() {
        assumeFalse(wireType == WireType.RAW);
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire w1 = wireType.apply(bytes);
        Map<String, String> value = new LinkedHashMap<>();
        value.put("a", "aya");
        value.put("b", "bee");
        try (DocumentContext dc = w1.writingDocument()) {
            dc.wire().write("map").object(value);
        }

        System.out.println(Wires.fromSizePrefixedBlobs(w1));
        try (DocumentContext dc = w1.readingDocument()) {
            Object o = dc.wire().read("map").object();
            assertEquals(value, o);
        }
    }

    static class My extends SelfDescribingMarshallable {
        List<CharSequence> stuff = new ArrayList<>();
        transient List<CharSequence> stuffBuffer = new ArrayList<>();

        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            wire.read("stuff").sequence(stuff, stuffBuffer, StringBuilder::new);
        }
    }
}
