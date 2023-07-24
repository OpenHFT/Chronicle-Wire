package net.openhft.chronicle.wire.internal.stream;

import net.openhft.chronicle.core.internal.util.RangeUtil;
import net.openhft.chronicle.core.values.DoubleValue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class StreamsUtilTest {

    @Test
    public void VanillaSpliterator_estimateSize_alwaysReturnsLongMaxValue() {
        Collection<Integer> collection = Collections.emptyList();
        StreamsUtil.VanillaSpliterator<Integer> spliterator = new StreamsUtil.VanillaSpliterator<>(collection.iterator());
        assertEquals(Long.MAX_VALUE, spliterator.estimateSize());
    }

    @Test
    public void VanillaSpliterator_trySplit_iteratorWithNoContentsShouldYieldNullSpliterator() {
        Collection<Integer> collection = Collections.emptyList();
        StreamsUtil.VanillaSpliterator<Integer> spliterator = new StreamsUtil.VanillaSpliterator<>(collection.iterator());
        Spliterator<Integer> split = spliterator.trySplit();
        assertNull(split);
    }

    @Test
    public void VanillaSpliterator_trySplit_batchSizeShouldBeCappedByTwoTimesBatchUnitIncrease() {
        Collection<Integer> collection = Stream.generate(() -> 1).limit(16777216).collect(Collectors.toList());
        StreamsUtil.VanillaSpliterator<Integer> spliterator = new StreamsUtil.VanillaSpliterator<>(collection.iterator());
        Spliterator<Integer> split = spliterator.trySplit();
        assertEquals(2 * 1024, split.getExactSizeIfKnown());
    }

    @Test
    public void VanillaSpliterator_trySplit_batchSizeSmallerThanMatchMaxSizeShouldBeReleased() {
        Collection<Integer> collection = Stream.generate(() -> 1).limit(10).collect(Collectors.toList());
        StreamsUtil.VanillaSpliterator<Integer> spliterator = new StreamsUtil.VanillaSpliterator<>(collection.iterator());
        Spliterator<Integer> split = spliterator.trySplit();
        assertEquals(10, split.getExactSizeIfKnown());
    }

    @Test
    public void VanillaSpliteratorOfLong_split() {
        PrimitiveIterator.OfLong iterator = LongStream.of(1, 2).iterator();
        StreamsUtil.VanillaSpliteratorOfLong spliterator = new StreamsUtil.VanillaSpliteratorOfLong(iterator);
        assertEquals(1, spliterator.split(1).getExactSizeIfKnown());
    }

    @Test
    public void VanillaSpliteratorOfDouble_split() {
        PrimitiveIterator.OfDouble iterator = DoubleStream.of(1, 2).iterator();
        StreamsUtil.VanillaSpliteratorOfDouble spliterator = new StreamsUtil.VanillaSpliteratorOfDouble(iterator);
        assertEquals(1, spliterator.split(1).getExactSizeIfKnown());
    }

    @Test
    public void VanillaSpliteratorOfDouble_trySplit_iteratorWithNoContentsShouldYieldNullSpliterator() {
        PrimitiveIterator.OfDouble iterator = DoubleStream.empty().iterator();
        StreamsUtil.VanillaSpliteratorOfDouble spliterator = new StreamsUtil.VanillaSpliteratorOfDouble(iterator);
        Spliterator.OfDouble split = spliterator.trySplit();
        assertNull(split);
    }

    @Test
    public void VanillaSpliteratorOfDouble_trySplit_batchSizeShouldBeCappedByTwoTimesBatchUnitIncrease() {
        PrimitiveIterator.OfDouble iterator = DoubleStream.generate(() -> 1).limit(16777216).iterator();
        StreamsUtil.VanillaSpliteratorOfDouble spliterator = new StreamsUtil.VanillaSpliteratorOfDouble(iterator);
        Spliterator.OfDouble split = spliterator.trySplit();
        assertEquals(2 * 1024, split.getExactSizeIfKnown());
    }

    @Test
    public void VanillaSpliteratorOfDouble_tryAdvance_nextValueShouldBeYielded() {
        PrimitiveIterator.OfDouble iterator = DoubleStream.generate(() -> 1).limit(16777216).iterator();
        StreamsUtil.VanillaSpliteratorOfDouble spliterator = new StreamsUtil.VanillaSpliteratorOfDouble(iterator);

        AtomicReference<Double> doubleReference = new AtomicReference<>();
        boolean result = spliterator.tryAdvance((DoubleConsumer) doubleReference::set);
        assertEquals(1d, doubleReference.get(), 0);
        assertTrue(result);
    }

    @Test
    public void VanillaSpliteratorOfDouble_tryAdvance_noValueYieldedForEmptyStream() {
        PrimitiveIterator.OfDouble iterator = DoubleStream.empty().iterator();
        StreamsUtil.VanillaSpliteratorOfDouble spliterator = new StreamsUtil.VanillaSpliteratorOfDouble(iterator);

        AtomicReference<Double> doubleReference = new AtomicReference<>();
        boolean result = spliterator.tryAdvance((DoubleConsumer) doubleReference::set);
        assertNull(doubleReference.get());
        assertFalse(result);
    }

    @Test
    public void VanillaSpliteratorOfDouble_estimateSize_alwaysReturnsLongMaxValue() {
        PrimitiveIterator.OfDouble iterator = DoubleStream.empty().iterator();
        StreamsUtil.VanillaSpliteratorOfDouble spliterator = new StreamsUtil.VanillaSpliteratorOfDouble(iterator);
        assertEquals(Long.MAX_VALUE, spliterator.estimateSize());
    }

}