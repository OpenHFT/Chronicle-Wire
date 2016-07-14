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
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by peter.lawrey on 06/02/2016.
 */
public class JSONWireTest {
    @Test
    public void testListFormatting() {
        JSONWire wire = new JSONWire(Bytes.elasticByteBuffer());

        List<Item> items = new ArrayList<>();
        items.add(new Item("item1", 1235666L, 1.1231231));
        items.add(new Item("item2", 2235666L, 1.0987987));
        items.add(new Item("item3", 3235666L, 1.12312));
        items.add(new Item("item4", 4235666L, 1.51231));

        WireOut out = wire.writeEventName(() -> "myEvent").list(items, Item.class);

        assertEquals("\"myEvent\":[{\"name\":\"item1\",\"number1\":1235666,\"number2\":1.1231231}," +
                "{\"name\":\"item2\",\"number1\":2235666,\"number2\":1.0987987}," +
                "{\"name\":\"item3\",\"number1\":3235666,\"number2\":1.12312}," +
                "{\"name\":\"item4\",\"number1\":4235666,\"number2\":1.51231}]", out.toString());
    }

    @Test
    public void testOpenBracket() {
        StringBuilder sb = new StringBuilder();

        JSONWire wire1 = new JSONWire(Bytes.from("\"echo\":\"Hello\"\n" +
                "\"echo2\":\"Hello2\"\n"));
        String text1 = wire1.readEventName(sb).text();
        assertEquals("echo", sb.toString());
        assertEquals("Hello", text1);
        String text2 = wire1.readEventName(sb).text();
        assertEquals("echo2", sb.toString());
        assertEquals("Hello2", text2);

        JSONWire wire2 = new JSONWire(Bytes.from("{ \"echoB\":\"HelloB\" }\n" +
                "{ \"echo2B\":\"Hello2B\" }\n"));
        String textB = wire2.readEventName(sb).text();
        assertEquals("echoB", sb.toString());
        assertEquals("HelloB", textB);
        String textB2 = wire2.readEventName(sb).text();
        assertEquals("echo2B", sb.toString());
        assertEquals("Hello2B", textB2);
    }

    @Test
    public void testNoSpaces() {
        JSONWire wire = new JSONWire(Bytes.from("\"echo\":\"\""));
        WireParser<Void> parser = new VanillaWireParser<>((s, v, $) -> System.out.println(s + " - " + v.text()));
        parser.parseOne(wire, null);
        assertEquals("", wire.bytes().toString());
    }

    @Test
    public void testMarshallableWithTwoLists() throws Exception {
        Bytes bytes = Bytes.elasticByteBuffer();
        JSONWire w = new JSONWire(bytes);

        TwoLists lists = new TwoLists("hi", 5, 5);
        w.writeEventName("hello_there").marshallable(lists);

        TwoLists readLists = new TwoLists();

        final StringBuilder sb = new StringBuilder();
        ValueIn valueIn = w.readEventName(sb);
        System.out.println(sb);

        valueIn.marshallable(readLists);
        System.out.println(readLists);
    }

    private static class Item extends AbstractMarshallable {
        String name;
        long number1;
        double number2;

        public Item(String name, long number1, double number2) {
            this.name = name;
            this.number1 = number1;
            this.number2 = number2;
        }
    }

    private static class TwoLists implements Marshallable {
        String name;
        List<Item> list1;
        List<Item> list2;

        public TwoLists() {
        }

        public TwoLists(String name, long number1, double number2) {
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
}
