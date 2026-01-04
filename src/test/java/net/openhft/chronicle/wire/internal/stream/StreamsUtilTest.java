/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.internal.stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.DoubleConsumer;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"deprecation", "removal"})
class StreamsUtilTest extends net.openhft.chronicle.wire.WireTestCommon {

    @Test
    @DisplayName("VanillaSpliterator estimateSize returns Long.MAX_VALUE")
    void vanillaSpliteratorEstimateSizeAlwaysReturnsLongMaxValue() {
        Collection<Integer> collection = Collections.emptyList();
        StreamsUtil.VanillaSpliterator<Integer> spliterator = new StreamsUtil.VanillaSpliterator<>(collection.iterator());
        assertEquals(Long.MAX_VALUE, spliterator.estimateSize(),
                "estimateSize should return Long.MAX_VALUE for VanillaSpliterator");
    }

    @Test
    @DisplayName("VanillaSpliterator trySplit returns null for empty collection iterator")
    void vanillaSpliteratorTrySplitIteratorWithNoContentsShouldYieldNullSpliterator() {
        Collection<Integer> collection = Collections.emptyList();
        StreamsUtil.VanillaSpliterator<Integer> spliterator = new StreamsUtil.VanillaSpliterator<>(collection.iterator());
        Spliterator<Integer> split = spliterator.trySplit();
        assertNull(split, "VanillaSpliterator trySplit should return null for empty iterator");
    }

    @Test
    @DisplayName("VanillaSpliterator trySplit should cap batch size")
    void vanillaSpliteratorTrySplitBatchSizeShouldBeCappedByTwoTimesBatchUnitIncrease() {
        Collection<Integer> collection = Stream.generate(() -> 1).limit(16777216).collect(Collectors.toList());
        StreamsUtil.VanillaSpliterator<Integer> spliterator = new StreamsUtil.VanillaSpliterator<>(collection.iterator());
        Spliterator<Integer> split = spliterator.trySplit();
        assertEquals(2 * 1024, split.getExactSizeIfKnown(),
                "VanillaSpliterator batch size should be capped at two times unit increase");
    }

    @Test
    @DisplayName("VanillaSpliterator trySplit uses actual size for small collections")
    void vanillaSpliteratorTrySplitBatchSizeSmallerThanMatchMaxSizeShouldBeReleased() {
        Collection<Integer> collection = Stream.generate(() -> 1).limit(10).collect(Collectors.toList());
        StreamsUtil.VanillaSpliterator<Integer> spliterator = new StreamsUtil.VanillaSpliterator<>(collection.iterator());
        Spliterator<Integer> split = spliterator.trySplit();
        assertEquals(10, split.getExactSizeIfKnown(),
                "Batch size should match small collection size");
    }

    @Test
    @DisplayName("VanillaSpliteratorOfLong split should return requested size")
    void vanillaSpliteratorOfLongSplit() {
        PrimitiveIterator.OfLong iterator = LongStream.of(1, 2).iterator();
        StreamsUtil.VanillaSpliteratorOfLong spliterator = new StreamsUtil.VanillaSpliteratorOfLong(iterator);
        assertEquals(1, spliterator.split(1).getExactSizeIfKnown(),
                "Long split size should match requested count");
    }

    @Test
    @DisplayName("VanillaSpliteratorOfDouble split should return requested size")
    void vanillaSpliteratorOfDoubleSplit() {
        PrimitiveIterator.OfDouble iterator = DoubleStream.of(1, 2).iterator();
        StreamsUtil.VanillaSpliteratorOfDouble spliterator = new StreamsUtil.VanillaSpliteratorOfDouble(iterator);
        assertEquals(1, spliterator.split(1).getExactSizeIfKnown(),
                "Double split size should match requested count");
    }

    @Test
    @DisplayName("VanillaSpliteratorOfDouble trySplit returns null for empty stream iterator")
    void vanillaSpliteratorOfDoubleTrySplitIteratorWithNoContentsShouldYieldNullSpliterator() {
        PrimitiveIterator.OfDouble iterator = DoubleStream.empty().iterator();
        StreamsUtil.VanillaSpliteratorOfDouble spliterator = new StreamsUtil.VanillaSpliteratorOfDouble(iterator);
        Spliterator.OfDouble split = spliterator.trySplit();
        assertNull(split, "VanillaSpliteratorOfDouble trySplit should return null for empty iterator");
    }

    @Test
    @DisplayName("VanillaSpliteratorOfDouble trySplit should cap batch size")
    void vanillaSpliteratorOfDoubleTrySplitBatchSizeShouldBeCappedByTwoTimesBatchUnitIncrease() {
        PrimitiveIterator.OfDouble iterator = DoubleStream.generate(() -> 1).limit(16777216).iterator();
        StreamsUtil.VanillaSpliteratorOfDouble spliterator = new StreamsUtil.VanillaSpliteratorOfDouble(iterator);
        Spliterator.OfDouble split = spliterator.trySplit();
        assertEquals(2 * 1024, split.getExactSizeIfKnown(),
                "Double spliterator batch size should be capped at two times unit increase");
    }

    @Test
    @DisplayName("VanillaSpliteratorOfDouble tryAdvance should yield the next value")
    void vanillaSpliteratorOfDoubleTryAdvanceNextValueShouldBeYielded() {
        PrimitiveIterator.OfDouble iterator = DoubleStream.generate(() -> 1).limit(16777216).iterator();
        StreamsUtil.VanillaSpliteratorOfDouble spliterator = new StreamsUtil.VanillaSpliteratorOfDouble(iterator);

        AtomicReference<Double> doubleReference = new AtomicReference<>();
        boolean result = spliterator.tryAdvance((DoubleConsumer) doubleReference::set);
        assertEquals(1d, doubleReference.get(), 0,
                "tryAdvance should supply the next double value");
        assertTrue(result, "tryAdvance should return true when a value is produced");
    }

    @Test
    @DisplayName("VanillaSpliteratorOfDouble tryAdvance should return false for empty stream iterator")
    void vanillaSpliteratorOfDoubleTryAdvanceNoValueYieldedForEmptyStream() {
        PrimitiveIterator.OfDouble iterator = DoubleStream.empty().iterator();
        StreamsUtil.VanillaSpliteratorOfDouble spliterator = new StreamsUtil.VanillaSpliteratorOfDouble(iterator);

        AtomicReference<Double> doubleReference = new AtomicReference<>();
        boolean result = spliterator.tryAdvance((DoubleConsumer) doubleReference::set);
        assertNull(doubleReference.get(), "No value should be produced for empty stream");
        assertFalse(result, "tryAdvance should return false when stream is empty");
    }

    @Test
    @DisplayName("VanillaSpliteratorOfDouble estimateSize returns Long.MAX_VALUE")
    void vanillaSpliteratorOfDoubleEstimateSizeAlwaysReturnsLongMaxValue() {
        PrimitiveIterator.OfDouble iterator = DoubleStream.empty().iterator();
        StreamsUtil.VanillaSpliteratorOfDouble spliterator = new StreamsUtil.VanillaSpliteratorOfDouble(iterator);
        assertEquals(Long.MAX_VALUE, spliterator.estimateSize(),
                "estimateSize should return Long.MAX_VALUE for VanillaSpliteratorOfDouble");
    }
}
