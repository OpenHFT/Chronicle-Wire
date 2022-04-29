package net.openhft.chronicle.wire.internal.streaming;

import net.openhft.chronicle.wire.MarshallableIn;
import net.openhft.chronicle.wire.Wire;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static net.openhft.chronicle.wire.internal.streaming.CreateUtil.createThenValueOuts;
import static org.junit.jupiter.api.Assertions.*;

class StreamsTest {


    @Test
    void streamTextMessage() {
        MarshallableIn tailer = createThenValueOuts(a -> a.writeString("Hello"));

        Stream<String> stream = Streams.of(tailer, (w, index) -> w.getValueIn().text());
        assertFalse(stream.isParallel());
        List<String> actualContent = stream.collect(toList());
        assertEquals(Collections.singletonList("Hello"), actualContent);
    }

    @Test
    void iteratorEmpty() {
        Wire tailer = CreateUtil.create();

        Iterator<String> iterator = Streams.iterator(tailer, (wire, index) -> "A");
        assertFalse(iterator.hasNext());
        assertFalse(iterator.hasNext());
    }

    @Test
    void iteratorTextMessage() {
        MarshallableIn tailer = createThenValueOuts(a -> a.writeString("Hello"));
        Iterator<String> iterator = Streams.iterator(tailer, (wire, index) -> wire.getValueIn().text());
        assertTrue(iterator.hasNext());
        // Make sure another call does not change the state
        assertTrue(iterator.hasNext());
        assertEquals("Hello", iterator.next());
        assertFalse(iterator.hasNext());
    }

}