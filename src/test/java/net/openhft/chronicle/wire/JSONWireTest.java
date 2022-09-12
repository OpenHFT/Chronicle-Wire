/*
 * Copyright 2016-2020 chronicle.software
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
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static junit.framework.TestCase.assertNull;
import static net.openhft.chronicle.wire.WireType.JSON;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JSONWireTest extends WireTestCommon {
    static void testCopyToBinaryAndBack(CharSequence str) {
        JSONWire json = new JSONWire(Bytes.from(str));
        HexDumpBytes hexDump = new HexDumpBytes();
        BinaryWire binary = new BinaryWire(hexDump);
        JSONWire json2 = new JSONWire(Bytes.allocateElasticOnHeap());
        json.copyTo(binary);
//        System.out.println(binary.bytes().toHexString());
        binary.copyTo(json2);
        assertEquals(
                str.toString()
                        .replaceAll("\\.0(\\D)", "$1")
                        .replaceAll(" ?\\[ ?", "[")
                        .replaceAll(" ?\\] ?", "]")
                ,
                json2.toString()
                        .replaceAll(" ?\\[ ?", "[")
                        .replaceAll(" ?\\] ?", "]")
        );
        hexDump.releaseLast();
    }

    @NotNull
    private JSONWire createWire(@NotNull String json) {
        return new JSONWire(Bytes.from(json));
    }

    @NotNull
    private JSONWire createWire() {
        return new JSONWire(Bytes.allocateElasticDirect()).useTypes(true);
    }

    @Test
    public void testOpenBracket() {
        @NotNull StringBuilder sb = new StringBuilder();

        @NotNull Wire wire1 = createWire("\"echo\":\"Hello\"\n\"echo2\":\"Hello2\"\n");
        @Nullable String text1 = wire1.readEventName(sb).text();
        assertEquals("echo", sb.toString());
        assertEquals("Hello", text1);
        @Nullable String text2 = wire1.readEventName(sb).text();
        assertEquals("echo2", sb.toString());
        assertEquals("Hello2", text2);

        @NotNull JSONWire wire2 = createWire("{ \"echoB\":\"HelloB\" }\n{ \"echo2B\":\"Hello2B\" }\n");
        @Nullable String textB = wire2.readEventName(sb).text();
        assertEquals("echoB", sb.toString());
        assertEquals("HelloB", textB);

        // finish up one object but keep reading.
        readExpect(wire2, '}');
        wire2.valueIn.stack.reset();

        @Nullable String textB2 = wire2.readEventName(sb).text();
        assertEquals("echo2B", sb.toString());
        assertEquals("Hello2B", textB2);
    }

    private void readExpect(Wire wire2, char expected) {
        wire2.consumePadding();
        assertEquals(expected, (char) wire2.bytes().readByte());
    }

    @Test
    public void testNoSpaces() {
        @NotNull Wire wire = createWire("\"echo\":\"\"");
        @NotNull VanillaWireParser parser = new VanillaWireParser(soutWireParselet(), VanillaWireParser.SKIP_READABLE_BYTES);
        parser.parseOne(wire);
        assertEquals("", wire.bytes().toString());
    }

    @NotNull
    private WireParselet soutWireParselet() {
        return (s, v) -> System.out.println(s + " - " + v.text());
    }

    @Test
    public void testListFormatting() {
        @NotNull Wire wire = createWire();

        @NotNull List<Item> items = new ArrayList<>();
        items.add(new Item("item1", 1235666L, 1.1231231));
        items.add(new Item("item2", 2235666L, 1.0987987));
        items.add(new Item("item3", 3235666L, 1.12312));
        items.add(new Item("item4", 4235666L, 1.51231));

        @NotNull WireOut out = wire.writeEventName(() -> "myEvent").list(items, Item.class);

        assertEquals("\"myEvent\":[{\"name\":\"item1\",\"number1\":1235666,\"number2\":1.1231231}," +
                "{\"name\":\"item2\",\"number1\":2235666,\"number2\":1.0987987}," +
                "{\"name\":\"item3\",\"number1\":3235666,\"number2\":1.12312}," +
                "{\"name\":\"item4\",\"number1\":4235666,\"number2\":1.51231}]", out.toString());

        testCopyToBinaryAndBack(out.toString());
        wire.bytes().releaseLast();
    }

    @Test
    public void testNullString() {
        @NotNull Wire w = createWire();

        @NotNull Item item1 = new Item(null, 1, 2);
        w.write("item").marshallable(item1);

        @NotNull Item item2 = new Item();
        w.read(() -> "item").marshallable(item2);

        assertNull(item2.name);
        assertEquals(item1, item2);
        assertEquals(item1.toString(), item2.toString());

        w.bytes().releaseLast();
    }

    @Test
    public void testBytes() {
        @NotNull Wire w = createWire();

        Bytes<?> bs = Bytes.from("blablabla");
        w.write("a").int64(123);
        w.write("somebytes").text(bs);

        Wire w2 = WireType.JSON.apply(w.bytes());
        assertEquals("\"a\":123,\"somebytes\":\"blablabla\"", w2.toString());

        Bytes<java.nio.ByteBuffer> bb = Bytes.elasticByteBuffer();
        assertEquals(123, w2.read("a").int64());

        w2.read("somebytes").text(bb);
        assertEquals(bs, bb);

        w2.bytes().releaseLast();
        bb.releaseLast();
    }

    @Test
    public void testFloatFromJson() {
        FooEvent foo = new FooEvent();
        foo.foo = 0.1f;
        @NotNull CharSequence str = WireType.JSON.asString(foo);
        assertEquals("{\"foo\":0.1}", str);
        // 0.1 when cast to a double is 0.10000000149011612. We used to throw an exception here because of this difference
        FooEvent foo2 = WireType.JSON.fromString(FooEvent.class, str);
        assertEquals(foo, foo2);
    }

    @Test
    public void testMarshallableWithTwoLists() {
        @NotNull Wire wire = createWire();

        @NotNull TwoLists lists1 = new TwoLists(null, 5, 5);
        wire.writeEventName("two_lists").marshallable(lists1);

        @NotNull TwoLists lists2 = new TwoLists();

        @NotNull final StringBuilder sb = new StringBuilder();
        @NotNull ValueIn valueIn = wire.readEventName(sb);

        valueIn.marshallable(lists2);

        // fails due to a trailing space if we don't call toString.
        // assertEquals(lists1, lists2);
        // assertEquals(lists1.toString(), lists2.toString());
        try {
            assertEquals("!net.openhft.chronicle.wire.JSONWireTest$TwoLists {\n" +
                    "  name: !!null \"\",\n" +
                    "  list1: [\n" +
                    "    { name: !!null \"\", number1: 0, number2: 0.0 },\n" +
                    "    { name: !!null \"\", number1: 1, number2: 10.0 },\n" +
                    "    { name: !!null \"\", number1: 2, number2: 20.0 },\n" +
                    "    { name: !!null \"\", number1: 3, number2: 30.0 },\n" +
                    "    { name: !!null \"\", number1: 4, number2: 40.0 }\n" +
                    "  ],\n" +
                    "  list2: [\n" +
                    "    { name: !!null \"\", number1: 0, number2: 0.0 },\n" +
                    "    { name: !!null \"\", number1: 1, number2: 10.0 },\n" +
                    "    { name: !!null \"\", number1: 2, number2: 20.0 },\n" +
                    "    { name: !!null \"\", number1: 3, number2: 30.0 },\n" +
                    "    { name: !!null \"\", number1: 4, number2: 40.0 }\n" +
                    "  ]\n" +
                    "}\n", lists1.toString());
            final String str = JSON.asString(lists1);
            testCopyToBinaryAndBack(str);
        } finally {
            wire.bytes().releaseLast();
        }
    }

    @Test
    public void testMapOfNamedKeys() {
        MapHolder mh = new MapHolder();
        Map<RetentionPolicy, Double> map = Collections.singletonMap(RetentionPolicy.CLASS, 0.1);
        mh.map = map;
        doTestMapOfNamedKeys(mh);
        mh.map = new TreeMap<>(map);
        doTestMapOfNamedKeys(mh);
        mh.map = new HashMap<>(map);
        doTestMapOfNamedKeys(mh);
        mh.map = new LinkedHashMap<>(map);
        doTestMapOfNamedKeys(mh);

        testCopyToBinaryAndBack("{\"map\":{\"CLASS\":0.1}}");
    }

    private void doTestMapOfNamedKeys(MapHolder mh) {
        assertEquals("{\"map\":{\"CLASS\":0.1}}",
                JSON.asString(mh));
    }

    @Test
    public void testDate() {
        Dates dates = new Dates();
        dates.date = LocalDate.of(2021, 5, 28);
        dates.dateTime = LocalDateTime.of(2020, 4, 26, 6, 35, 11);
        dates.zdateTime = ZonedDateTime.of(dates.dateTime, ZoneId.of("UTC"));
        @NotNull CharSequence str = WireType.JSON.asString(dates);
        String expected = "{\"date\":\"2021-05-28\",\"dateTime\":\"2020-04-26T06:35:11\",\"zdateTime\":\"2020-04-26T06:35:11Z[UTC]\"}";
        assertEquals(expected, str);
        JSONWire jw = new JSONWire(Bytes.allocateElasticOnHeap());
        jw.trimFirstCurly(false);
        jw.getValueOut().typedMarshallable(dates);
        assertEquals(expected, jw.toString());

        testCopyToBinaryAndBack(str);
    }

    @Test
    public void commaIsNotInAValue() {
        String text = "[1,2,3]";
        Wire wire = createWire();
        wire.bytes().append(text);
        final Object list = wire.getValueIn().object();
        assertEquals("[1, 2, 3]", "" + list);

        String text2 = "[ 1, 2, 3 ]";
        wire.bytes().clear().append(text2);
        final Object list2 = wire.getValueIn().object();
        assertEquals("[1, 2, 3]", "" + list2);
    }

    @Test
    public void testArrayInDictionary() {
        String text = "[320,{\"as\":[1,2,3]}]";
        final JSONWire jsonWire = new JSONWire(Bytes.from(text));
        final Object list = jsonWire.getValueIn().object();
        assertEquals("[320, {as=[1, 2, 3]}]", "" + list);
    }

    @Test
    public void testDateNull() {
        Dates dates = new Dates();
        @NotNull CharSequence str = WireType.JSON.asString(dates);
        assertEquals("{\"date\":null,\"dateTime\":null,\"zdateTime\":null}", str);
        testCopyToBinaryAndBack(str);
    }

    @Test
    public void testArrayInDictionary2() {
        String text = "[320,{\"as\":[[\"32905.50000\",\"1.60291699\",\"1625822573.857656\"],[\"32905.60000\",\"0.10415889\",\"1625822573.194909\"]],\"bs\":[[\"32893.60000\",\"0.15042948\",\"1625822574.220475\"]]},\"book-10\"]";
        final JSONWire jsonWire = new JSONWire(Bytes.from(text));
        final Object list = jsonWire.getValueIn().object();
        assertEquals("[320, {as=[[32905.50000, 1.60291699, 1625822573.857656], [32905.60000, 0.10415889, 1625822573.194909]], bs=[[32893.60000, 0.15042948, 1625822574.220475]]}, book-10]", "" + list);
        testCopyToBinaryAndBack(text);
    }

    @Test
    public void testArrayDelimiterNoSpace() {
        // This fails
        String text = "[320,{\"as\":[[\"32905.50000\",\"1.60291699\",\"1625822573.857656\"],[\"32905.60000\",\"0.10415889\",\"1625822573.194909\"]],\"bs\":[[\"32893.60000\",\"0.15042948\",\"1625822574.220475\"]]},\"book-10\"]";

        // Simple version
//        String text = "[1,{\"a\":[2,3]}]";

        // This works, for some reason
//        String text = "[1,2,3,\"c\"]";

        final Bytes<ByteBuffer> byteBufferBytes = Bytes.elasticByteBuffer();
        byteBufferBytes.append(text);

        final JSONWire jsonWire = new JSONWire(byteBufferBytes);

        final List<Object> list = jsonWire.getValueIn().list(Object.class);
        assertNotNull(list);
        testCopyToBinaryAndBack(text);
    }

    static class MapWithIntegerKeysHolder extends SelfDescribingMarshallable {
        Map<Integer, String> intMap = new LinkedHashMap<>();
        Map<Long, String> longMap = new LinkedHashMap<>();
        Map<Double, String> doubleMap = new LinkedHashMap<>();
    }

    @Test
    public void nestedMapWithIntegerKeys() {
        MapWithIntegerKeysHolder mh = new MapWithIntegerKeysHolder();
        mh.intMap.put(1111, "ones");
        mh.intMap.put(2222, "twos");
        mh.longMap.put(888888888888L, "eights");
        mh.longMap.put(999999999999L, "nines");
        mh.doubleMap.put(1.28, "number");
        mh.doubleMap.put(2.56, "number");
        final String text = JSON.asString(mh);
        assertEquals("" +
                        "{\"intMap\":{\"1111\":\"ones\",\"2222\":\"twos\"},\"longMap\":{\"888888888888\":\"eights\",\"999999999999\":\"nines\"},\"doubleMap\":{\"1.28\":\"number\",\"2.56\":\"number\"}}",
                text);
        MapWithIntegerKeysHolder mh2 = JSON.fromString(MapWithIntegerKeysHolder.class, text);
        assertEquals(mh, mh2);
        testCopyToBinaryAndBack(text);
    }

    @Test
    public void testWritingLayout() {
        final Bytes<byte[]> bytes = Bytes.allocateElasticOnHeap(1024);
        final JSONWire wire = new JSONWire(bytes, true);

        final Value foo = new Value();

        wire.getValueOut().marshallable(foo);

        assertEquals("{\"a\":{\"b\":\"c\"}}", bytes.toString());
    }

    private static class Value extends SelfDescribingMarshallable {
        final Inner a = new Inner();

        private static class Inner extends SelfDescribingMarshallable {
            String b = "c";
        }
    }

    private static class FooEvent extends AbstractEventCfg<FooEvent> {
        float foo;
    }

    private static class Item extends SelfDescribingMarshallable {
        String name;
        long number1;
        double number2;

        Item() {
        }

        Item(String name, long number1, double number2) {
            this.name = name;
            this.number1 = number1;
            this.number2 = number2;
        }
    }

    private static class TwoLists implements Marshallable {
        @Nullable
        String name;
        List<Item> list1;
        List<Item> list2;

        TwoLists() {
        }

        TwoLists(@Nullable String name, long number1, double number2) {
            this.name = name;
            this.list1 = new ArrayList<>();
            for (int i = 0; i < number1; i++) {
                list1.add(new Item(name, i, i * 10));
            }
            this.list2 = new ArrayList<>();
            for (int i = 0; i < number2; i++) {
                list2.add(new Item(name, i, i * 10));
            }
        }

        @Override
        public String toString() {
            return Marshallable.$toString(this);
        }

        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            name = wire.read(() -> "name").text();
            list1 = wire.read(() -> "list1").list(Item.class);
            list2 = wire.read(() -> "list2").list(Item.class);
        }

        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            wire.write(() -> "name").text(name);
            wire.write(() -> "list1").list(list1, Item.class);
            wire.write(() -> "list2").list(list2, Item.class);
        }
    }

    static class MapHolder extends SelfDescribingMarshallable {

        Map<RetentionPolicy, Double> map;
    }

    private static class Dates extends SelfDescribingMarshallable {
        LocalDate date;
        LocalDateTime dateTime;
        ZonedDateTime zdateTime;

        Dates() {
        }
    }
}
