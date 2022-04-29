package net.openhft.chronicle.wire.internal.streaming.reduction;

import net.openhft.chronicle.wire.MarshallableIn;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.internal.streaming.Reduction;
import org.junit.Test;

import java.util.function.LongSupplier;

import static net.openhft.chronicle.wire.internal.streaming.CreateUtil.createThenValueOuts;
import static net.openhft.chronicle.wire.internal.streaming.Reductions.reducingLong;
import static net.openhft.chronicle.wire.internal.streaming.ToLongDocumentExtractor.extractingIndex;
import static org.junit.Assert.assertEquals;

public class LastIndexSeenTest extends WireTestCommon {

    @Test
    public void lastIndexSeen() {
        Reduction<LongSupplier> listener = reducingLong(extractingIndex(), 0, (a, b) -> b);

        test(listener);

        long indexLastSeen = listener.reduction().getAsLong();
        assertEquals("16", Long.toHexString(indexLastSeen));
    }

    @Test
    public void minAndMaxIndexSeen() {
        Reduction<LongSupplier> minListener = reducingLong(extractingIndex(), Long.MAX_VALUE, Math::min);
        Reduction<LongSupplier> maxListener = reducingLong(extractingIndex(), Long.MIN_VALUE, Math::max);

        test(minListener);
        test(maxListener);

        long min = minListener.reduction().getAsLong();
        long max = maxListener.reduction().getAsLong();

        assertEquals("4", Long.toHexString(min));
        assertEquals("16", Long.toHexString(max));
    }

    private void test(Reduction<?> listener) {
        // Add stuff that simulated existing values in the queue
        MarshallableIn tailer = createThenValueOuts(
                a -> a.writeString("one"),
                a -> a.writeString("two"),
                a -> a.writeString("three")
        );
        listener.accept(tailer);

    }

}