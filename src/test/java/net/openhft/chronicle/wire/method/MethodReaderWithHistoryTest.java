package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import net.openhft.chronicle.wire.utils.RecordHistory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// Test class extending WireTestCommon to test MethodReader functionality with history recording
public class MethodReaderWithHistoryTest extends WireTestCommon {

    // Test with text wire type
    @Test
    public void text() {
        doTest(WireType.TEXT);
    }

    // Test with YAML-only wire type
    @Test
    public void yaml() {
        doTest(WireType.YAML_ONLY);
    }

    // Test with binary wire type
    @Test
    public void binary() {
        doTest(WireType.BINARY);
    }

    // Helper method to perform tests with different wire types
    private void doTest(WireType wireType) {
        // Create a new wire instance of the specified wire type
        Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());

        // Create a method writer for the RecordHistorySays interface
        RecordHistorySays historySays = wire.methodWriter(RecordHistorySays.class);

        // Initialize a message history instance
        net.openhft.chronicle.wire.VanillaMessageHistory history = new net.openhft.chronicle.wire.VanillaMessageHistory();

        // Record and write the first history and message
        history.reset();
        history.addSource(1, 11);
        historySays.history(history).say("hello");

        // Record and write the second history and message
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

        // Read and validate the second message and its history
        String[] says2 = {null};
        MethodReader reader2 = wire.methodReader((RecordHistorySays) h -> {
            assertEquals(2, h.sourceId(0));
            assertEquals(22, h.sourceIndex(0));
            return (Saying) s -> says2[0] = s;
        });

        assertTrue(reader2.readOne());
        assertEquals("bye", says2[0]);
    }

    // Interface for a saying action
    interface Saying {
        void say(String say);
    }

    // Interface extending RecordHistory for the Saying interface
    public interface RecordHistorySays extends RecordHistory<Saying> {
    }
}
