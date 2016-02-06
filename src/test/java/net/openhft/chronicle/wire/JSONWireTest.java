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
}
