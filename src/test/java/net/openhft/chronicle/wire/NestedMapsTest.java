/*
 * Copyright 2016 higherfrequencytrading.com
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

import static java.util.Collections.addAll;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by peter on 21/04/16.
 */
@RunWith(value = Parameterized.class)
public class NestedMapsTest {
    private final WireType wireType;

    public NestedMapsTest(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{WireType.TEXT},
                new Object[]{WireType.BINARY},
                new Object[]{WireType.FIELDLESS_BINARY}
        );
    }

    @Test
    public void testMapped() {
        Mapped m = new Mapped();
        addAll(m.words, "A quick brown fox jumps over the lazy dog".split(" "));
        addAll(m.numbers, 1, 2, 2, 3, 5, 8, 13);
        m.map1.put("aye", "AAA");
        m.map1.put("bee", "BBB");
        m.map2.put("one", 1.0);
        m.map2.put("two point two", 2.2);

        Bytes bytes = Bytes.elasticByteBuffer();
        Wire wire = wireType.apply(bytes);
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
                        "    1, 2, 2, 3, 5, 8, 13\n" +
                        "  ],\n" +
                        "  map1: {\n" +
                        "    aye: AAA, bee: BBB\n" +
                        "  },\n" +
                        "  map2: {\n" +
                        "    one: 1.0, two point two: 2.2\n" +
                        "  }\n" +
                        "}\n", Wires.fromSizePrefixedBlobs(wire));
                break;
            case BINARY:
                assertEquals("--- !!data #binary\n" +
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
                        "  map1: [\n" +
                        "    aye: AAA,\n" +
                        "    bee: BBB\n" +
                        "  ],\n" +
                        "  map2: [\n" +
                        "    one: 1.0,\n" +
                        "    two point two: 2.2\n" +
                        "  ]\n" +
                        "}\n", Wires.fromSizePrefixedBlobs(wire));
                break;
            case FIELDLESS_BINARY:
                assertEquals("--- !!data #binary\n" +
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
                        "  [\n" +
                        "    aye: AAA,\n" +
                        "    bee: BBB\n" +
                        "  ],\n" +
                        "  [\n" +
                        "    one: 1.0,\n" +
                        "    two point two: 2.2\n" +
                        "  ]\n" +
                        "]\n", Wires.fromSizePrefixedBlobs(wire));
                break;
        }
        Mapped m2 = new Mapped();
        assertTrue(wire.readDocument(null, w -> w.read(() -> "mapped").marshallable(m2)));
        assertEquals(m, m2);
    }

    @Test
    public void testMappedTopLevel() {
        Mapped m = new Mapped();
        addAll(m.words, "A quick brown fox jumps over the lazy dog".split(" "));
        addAll(m.numbers, 1, 2, 2, 3, 5, 8, 13);
        m.map1.put("aye", "AAA");
        m.map1.put("bee", "BBB");
        m.map2.put("one", 1.0);
        m.map2.put("two point two", 2.2);

        Bytes bytes = Bytes.elasticByteBuffer();
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
                        "  1, 2, 2, 3, 5, 8, 13\n" +
                        "],\n" +
                        "map1: {\n" +
                        "  aye: AAA, bee: BBB\n" +
                        "}\n" +
                        "map2: {\n" +
                        "  one: 1.0, two point two: 2.2\n" +
                        "}\n", wire.toString());
                break;
            case BINARY:
                assertEquals("[pos: 0, rlim: 149, wlim: 8EiB, cap: 8EiB ] ‖" +
                        "Åwords\\u0082*٠٠٠áAåquickåbrownãfoxåjumpsäoverãtheälazyãdog" +
                        "Çnumbers\\u0082⒎٠٠٠⒈⒉⒉⒊⒌⒏⒔" +
                        "Ämap1\\u0082⒙٠٠٠¹⒊ayeãAAA¹⒊beeãBBB" +
                        "Ämap2\\u0082&٠٠٠¹⒊one\\u0091٠٠٠٠٠٠ð?¹⒔two point two\\u0091\\u009A\\u0099\\u0099\\u0099\\u0099\\u0099⒈@" +
                        "‡٠٠٠٠٠٠٠٠", wire.bytes().toDebugString());
                break;
            case FIELDLESS_BINARY:
                assertEquals("[pos: 0, rlim: 125, wlim: 8EiB, cap: 8EiB ] ‖" +
                        "\\u0082*٠٠٠áAåquickåbrownãfoxåjumpsäoverãtheälazyãdog" +
                        "\\u0082⒎٠٠٠⒈⒉⒉⒊⒌⒏⒔\\u0082⒙٠٠٠¹⒊ayeãAAA¹⒊beeãBBB" +
                        "\\u0082&٠٠٠¹⒊one\\u0091٠٠٠٠٠٠ð?¹⒔two point two\\u0091\\u009A\\u0099\\u0099\\u0099\\u0099\\u0099⒈@" +
                        "‡٠٠٠٠٠٠٠٠", wire.bytes().toDebugString());
                break;
        }
        Mapped m2 = new Mapped();
        m2.readMarshallable(wire);
        assertEquals(m, m2);
    }

    static class Mapped extends AbstractMarshallable {
        final Set<String> words = new LinkedHashSet<>();
        final List<Integer> numbers = new ArrayList<>();
        final Map<String, String> map1 = new LinkedHashMap<>();
        final Map<String, Double> map2 = new LinkedHashMap<>();
    }
}
