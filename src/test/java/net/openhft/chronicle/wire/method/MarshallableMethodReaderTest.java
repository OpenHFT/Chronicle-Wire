package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;
import org.junit.Test;

public class MarshallableMethodReaderTest {
    @Test
    public void test() {
        Wire wire = new TextWire(Bytes.from("say: hi")).useTextDocuments();
        wire.methodReader(new SayingMicroservice()).readOne();
    }

    interface Saying {
        void say(String hi);
    }

    static class SayingMicroservice extends SelfDescribingMarshallable implements Saying {
        @Override
        public void say(String hi) {
            System.out.println("say: " + hi);
        }
    }
}
