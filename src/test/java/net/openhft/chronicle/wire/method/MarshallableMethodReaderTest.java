package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MarshallableMethodReaderTest {
    @Test
    public void test() {
        Wire wire = new TextWire(Bytes.from("say: hi")).useTextDocuments();
        final SayingMicroservice sm = new SayingMicroservice();
        final MethodReader reader = wire.methodReader(sm);
        assertTrue(reader.readOne());
    }

    @Test
    public void ignoredMethods() {
        Wire wire = Wire.newYamlWireOnHeap();
        final SayingMicroservice sm = new SayingMicroservice();
        final MethodReader reader = wire.methodReader(sm);
        for (Method method : sm.getClass().getMethods()) {
            final String name = method.getName();
            wire.write(name).text("");
            assertTrue(method.toString(), reader.readOne());
        }
    }

    interface Saying {
        void say(String hi);
    }

    static class SayingMicroservice extends SelfDescribingMarshallable implements Saying {
        transient List<String> said = new ArrayList<>();

        @Override
        public void say(String hi) {
            said.add(hi);
        }
    }
}
