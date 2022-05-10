package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.wire.MarshallableOut;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class StringConsumerMarshallableOutTest {
    @Test
    public void saysYaml() {
        final WireType wireType = WireType.YAML_ONLY;
        final String expected = "" +
                "say: One\n" +
                "...\n" +
                "say: Two\n" +
                "...\n" +
                "say: Three\n" +
                "...\n";
        doTest(wireType, expected);
    }

    @Test
    public void saysJson() {
        final WireType wireType = WireType.JSON_ONLY;
        final String expected = "" +
                "\"say\":\"One\"\n" +
                "\"say\":\"Two\"\n" +
                "\"say\":\"Three\"\n";
        doTest(wireType, expected);
    }

    private void doTest(WireType wireType, String expected) {
        StringWriter sw = new StringWriter();
        MarshallableOut out = new StringConsumerMarshallableOut(s -> {
            sw.append(s);
            if (!s.endsWith("\n"))
                sw.append('\n');
        }, wireType);
        final Says says = out.methodWriter(Says.class);
        says.say("One");
        says.say("Two");
        says.say("Three");
        assertEquals(expected,
                sw.toString());
    }

    interface Says {
        void say(String text);
    }
}