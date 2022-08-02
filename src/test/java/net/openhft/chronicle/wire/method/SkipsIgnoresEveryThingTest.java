package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.util.IgnoresEverything;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class SkipsIgnoresEveryThingTest {
    @Test
    public void selective() {
        String text = "" +
                "to: 1\n" +
                "say: hi\n" +
                "...\n" +
                "to: 2\n" +
                "say: bad\n" +
                "...\n" +
                "to: 3\n" +
                "say: fine\n" +
                "...\n" +
                "to: 4\n" +
                "say: bad\n" +
                "...\n";

        Wire wire = new TextWire(Bytes.from(text)).useTextDocuments();
        List<String> words = new ArrayList<>();
        final MethodReader reader = wire.methodReader(new Selective() {
            DontSayBad dsb = new DontSayBad();

            @Override
            public Saying to(long id) {
                if (id % 2 == 0)
                    return dsb;
                return words::add;
            }
        });
        for (int i = 4; i >= 0; i--)
            assertEquals(i > 0, reader.readOne());
        assertEquals("[hi, fine]", words.toString());
    }

    interface Selective {
        Saying to(long id);
    }

    interface Saying {
        void say(String text);
    }

    static class DontSayBad implements Saying, IgnoresEverything {
        @Override
        public void say(String text) {
            assertNotEquals("bad", text);
        }
    }

}