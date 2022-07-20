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
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

import static java.util.Collections.addAll;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("rawtypes")
@RunWith(value = Parameterized.class)
public class NestedMapsTest extends WireTestCommon {
    private final WireType wireType;

    public NestedMapsTest(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{WireType.JSON},
                new Object[]{WireType.TEXT},
                new Object[]{WireType.BINARY},
                new Object[]{WireType.FIELDLESS_BINARY}
        );
    }

    @SuppressWarnings("incomplete-switch")
    @Test
    public void testMapped() {
        @NotNull Mapped m = new Mapped();
        addAll(m.words, "A quick brown fox jumps over the lazy dog".split(" "));
        addAll(m.numbers, 1, 2, 2, 3, 5, 8, 13);
        m.map1.put("aye", "AAA");
        m.map1.put("bee", "BBB");
        m.map2.put("one", 1.0);
        m.map2.put("two point two", 2.2);

        Bytes<?> bytes = Bytes.allocateElasticOnHeap(128);
        Wire wire = wireType.apply(bytes);
        wire.usePadding(false);

        wire.writeDocument(false, w -> w.writeEventName("mapped").object(m));
        switch (wireType) {
            case TEXT:
                assertEquals("--- !!data\n" +
                        "mapped: !net.openhft.chronicle.wire.NestedMapsTest$Mapped {\n" +
                        "  words: [\n" +
                        "    A,\n" +
                        "    quick,\n" +
                        "    brown,\n" +
                        "    fox,\n" +
                        "    jumps,\n" +
                        "    over,\n" +
                        "    the,\n" +
                        "    lazy,\n" +
                        "    dog\n" +
                        "  ],\n" +
                        "  numbers: [\n" +
                        "    1,\n" +
                        "    2,\n" +
                        "    2,\n" +
                        "    3,\n" +
                        "    5,\n" +
                        "    8,\n" +
                        "    13\n" +
                        "  ],\n" +
                        "  map1: {\n" +
                        "    aye: AAA,\n" +
                        "    bee: BBB\n" +
                        "  },\n" +
                        "  map2: {\n" +
                        "    one: 1.0,\n" +
                        "    two point two: 2.2\n" +
                        "  }\n" +
                        "}\n", Wires.fromSizePrefixedBlobs(wire));
                break;
            case BINARY:
                assertEquals("" +
                        "--- !!data #binary\n" +
                        "mapped: !net.openhft.chronicle.wire.NestedMapsTest$Mapped {\n" +
                        "  words: [\n" +
                        "    A,\n" +
                        "    quick,\n" +
                        "    brown,\n" +
                        "    fox,\n" +
                        "    jumps,\n" +
                        "    over,\n" +
                        "    the,\n" +
                        "    lazy,\n" +
                        "    dog\n" +
                        "  ],\n" +
                        "  numbers: [\n" +
                        "    1,\n" +
                        "    2,\n" +
                        "    2,\n" +
                        "    3,\n" +
                        "    5,\n" +
                        "    8,\n" +
                        "    13\n" +
                        "  ],\n" +
                        "  map1: {\n" +
                        "    aye: AAA,\n" +
                        "    bee: BBB\n" +
                        "  },\n" +
                        "  map2: {\n" +
                        "    one: 1,\n" +
                        "    two point two: 2.2\n" +
                        "  }\n" +
                        "}\n", Wires.fromSizePrefixedBlobs(wire));
                break;
            case FIELDLESS_BINARY:
                assertEquals("" +
                        "--- !!data #binary\n" +
                        "mapped: !net.openhft.chronicle.wire.NestedMapsTest$Mapped [\n" +
                        "  [\n" +
                        "    A,\n" +
                        "    quick,\n" +
                        "    brown,\n" +
                        "    fox,\n" +
                        "    jumps,\n" +
                        "    over,\n" +
                        "    the,\n" +
                        "    lazy,\n" +
                        "    dog\n" +
                        "  ],\n" +
                        "  [\n" +
                        "    1,\n" +
                        "    2,\n" +
                        "    2,\n" +
                        "    3,\n" +
                        "    5,\n" +
                        "    8,\n" +
                        "    13\n" +
                        "  ],\n" +
                        "  {\n" +
                        "    aye: AAA,\n" +
                        "    bee: BBB\n" +
                        "  },\n" +
                        "  {\n" +
                        "    one: 1,\n" +
                        "    two point two: 2.2\n" +
                        "  }\n" +
                        "]\n", Wires.fromSizePrefixedBlobs(wire));
                break;
        }
        @NotNull Mapped m2 = new Mapped();
        assertTrue(wire.readDocument(null, w -> w.read(() -> "mapped")
                .marshallable(m2)));
        assertEquals(m, m2);

        bytes.releaseLast();
    }

    @SuppressWarnings("incomplete-switch")
    @Test
    public void testMappedTopLevel() {
        @NotNull Mapped m = new Mapped();
        addAll(m.words, "A quick brown fox jumps over the lazy dog".split(" "));
        addAll(m.numbers, 1, 2, 2, 3, 5, 8, 13);
        m.map1.put("aye", "AAA");
        m.map1.put("bee", "BBB");
        m.map2.put("one", 1.0);
        m.map2.put("two point two", 2.2);

        Bytes<?> bytes = Bytes.elasticHeapByteBuffer(128);
        Wire wire = wireType.apply(bytes);
        m.writeMarshallable(wire);
        switch (wireType) {
            case TEXT:
                assertEquals("words: [\n" +
                        "  A,\n" +
                        "  quick,\n" +
                        "  brown,\n" +
                        "  fox,\n" +
                        "  jumps,\n" +
                        "  over,\n" +
                        "  the,\n" +
                        "  lazy,\n" +
                        "  dog\n" +
                        "],\n" +
                        "numbers: [\n" +
                        "  1,\n" +
                        "  2,\n" +
                        "  2,\n" +
                        "  3,\n" +
                        "  5,\n" +
                        "  8,\n" +
                        "  13\n" +
                        "],\n" +
                        "map1: {\n" +
                        "  aye: AAA,\n" +
                        "  bee: BBB\n" +
                        "}\n" +
                        "map2: {\n" +
                        "  one: 1.0,\n" +
                        "  two point two: 2.2\n" +
                        "}\n", wire.toString());
                break;
            case BINARY:
                assertEquals("[pos: 0, rlim: 143, wlim: 2147483632, cap: 2147483632 ] ǁÅwords\\u0082*٠٠٠áAåquickåbrownãfoxåjumpsäoverãtheälazyãdogÇnumbers\\u0082⒕٠٠٠¡⒈¡⒉¡⒉¡⒊¡⒌¡⒏¡⒔Ämap1\\u0082⒙٠٠٠¹⒊ayeãAAA¹⒊beeãBBBÄmap2\\u0082\\u0019٠٠٠¹⒊one¡⒈¹⒔two point two\\u0092Ü⒈‡٠٠٠٠٠٠٠٠", wire.bytes().toDebugString());
                break;
            case FIELDLESS_BINARY:
                assertEquals("[pos: 0, rlim: 119, wlim: 2147483632, cap: 2147483632 ] ǁ\\u0082*٠٠٠áAåquickåbrownãfoxåjumpsäoverãtheälazyãdog\\u0082⒕٠٠٠¡⒈¡⒉¡⒉¡⒊¡⒌¡⒏¡⒔\\u0082⒙٠٠٠¹⒊ayeãAAA¹⒊beeãBBB\\u0082\\u0019٠٠٠¹⒊one¡⒈¹⒔two point two\\u0092Ü⒈‡٠٠٠٠٠٠٠٠٠", wire.bytes().toDebugString());
                break;
        }
        @NotNull Mapped m2 = new Mapped();
        m2.readMarshallable(wire);
        assertEquals(m, m2);

        bytes.releaseLast();
    }

    @Test
    public void testMapReadAndWrite() {
        Bytes<?> bytes = Bytes.elasticByteBuffer();
        Wire wire = wireType.apply(bytes);
        wire.usePadding(wire.isBinary());

        @NotNull final Map<Integer, Integer> expected = new HashMap<>();
        expected.put(1, 2);
        expected.put(2, 2);
        expected.put(3, 3);

        wire.writeMap(expected);
        @NotNull final Map<Integer, Integer> actual = wire.readMap();
        bytes.releaseLast();
        if (wireType == WireType.JSON)
            assertEquals(expected.toString(), actual.toString());
        else
            assertEquals(expected, actual);
    }

    static class Mapped extends SelfDescribingMarshallable {
        final Set<String> words = new LinkedHashSet<>();
        final List<Integer> numbers = new ArrayList<>();
        final Map<String, String> map1 = new LinkedHashMap<>();
        final Map<String, Double> map2 = new LinkedHashMap<>();
    }
}
