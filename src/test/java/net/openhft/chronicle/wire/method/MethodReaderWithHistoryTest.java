package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.VanillaMessageHistory;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import net.openhft.chronicle.wire.utils.RecordHistory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MethodReaderWithHistoryTest extends WireTestCommon {
    @Test
    public void text() {
        doTest(WireType.TEXT);
    }

    @Test
    public void yaml() {
        doTest(WireType.YAML_ONLY);
    }

    @Test
    public void binary() {
        doTest(WireType.BINARY);
    }

    private void doTest(WireType wireType) {
        Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        RecordHistorySays historySays = wire.methodWriter(RecordHistorySays.class);
        VanillaMessageHistory history = new VanillaMessageHistory();

        history.reset();
        history.addSource(1, 11);
        historySays.history(history).say("hello");

        history.reset();
        history.addSource(2, 22);
        historySays.history(history).say("bye");

//        System.out.println(wire);

        String[] says = {null};
        MethodReader reader = wire.methodReader((RecordHistorySays) h -> {
            assertEquals(1, h.sourceId(0));
            assertEquals(11, h.sourceIndex(0));
            return (Saying) s -> says[0] = s;
        });
        assertTrue(reader.readOne());
        assertEquals("hello", says[0]);

        String[] says2 = {null};
        MethodReader reader2 = wire.methodReader((RecordHistorySays) h -> {
            assertEquals(2, h.sourceId(0));
            assertEquals(22, h.sourceIndex(0));
            return (Saying) s -> says2[0] = s;
        });
        assertTrue(reader2.readOne());
        assertEquals("bye", says2[0]);
    }

    interface Saying {
        void say(String say);
    }

    public interface RecordHistorySays extends RecordHistory<Saying> {
    }
}
