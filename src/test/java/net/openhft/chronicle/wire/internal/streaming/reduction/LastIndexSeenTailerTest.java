package net.openhft.chronicle.wire.internal.streaming.reduction;

import net.openhft.chronicle.wire.MarshallableIn;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.internal.streaming.Reduction;
import net.openhft.chronicle.wire.internal.streaming.Reductions;
import net.openhft.chronicle.wire.internal.streaming.ToLongDocumentExtractor;
import org.junit.Test;

import java.util.function.LongSupplier;

import static net.openhft.chronicle.wire.internal.streaming.CreateUtil.createThenValueOuts;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LastIndexSeenTailerTest extends WireTestCommon {

    @Test
    public void lastIndexSeenTailer() {

        // Add stuff that simulated existing values in the queue
        MarshallableIn tailer = createThenValueOuts(
                a -> a.writeString("one"),
                a -> a.writeString("two"),
                a -> a.writeString("three")
        );

        final Reduction<LongSupplier> listener = Reductions.reducingLong(ToLongDocumentExtractor.extractingIndex(), 0, (a, b) -> b);
        listener.accept(tailer);

        assertTrue(listener.reduction().getAsLong() > 3 + 3 + 5);

    }
}