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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by peter.lawrey on 06/02/2016.
 */
public class JSONWireTest {
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
}
