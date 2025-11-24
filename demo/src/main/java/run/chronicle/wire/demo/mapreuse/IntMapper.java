//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package run.chronicle.wire.demo.mapreuse;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

import static java.util.Comparator.comparingInt;

/**
 * Maps integer keys to values using a sorted list so that existing objects can
 * be reused when deserialising.
 */
public class IntMapper<V> extends SelfDescribingMarshallable {

    private final List<V> values = new ArrayList<>();
    private final ToIntFunction<? super V> extractor;

    public IntMapper(final ToIntFunction<? super V> extractor) {
        this.extractor = Objects.requireNonNull(extractor);
    }

    public List<V> values() { return values; }

    public IntStream keys() {
        return values.stream().mapToInt(extractor);
    }

    public void set(Collection<? extends V> values) {
        this.values.clear();
        this.values.addAll(values);
        // Sort the list in id order
        this.values.sort(comparingInt(extractor));
    }

    /**
     * Looks up a value by key.
     *
     * @param key integer identifier
     * @return the mapped value or {@code null} if absent
     */
    public V get(int key) {
        int index = binarySearch(key);
        if (index >= 0)
            return values.get(index);
        else
            return null;
    }

    int binarySearch(final int key) {
        int low = 0;
        int high = values.size() - 1;

        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final V midVal = values.get(mid);
            int cmp = Integer.compare(
                    extractor.applyAsInt(midVal), key);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid;
        }
        return -(low + 1);
    }

}
