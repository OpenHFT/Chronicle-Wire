/*
 *
 *  *     Copyright (C) ${YEAR}  higherfrequencytrading.com
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
