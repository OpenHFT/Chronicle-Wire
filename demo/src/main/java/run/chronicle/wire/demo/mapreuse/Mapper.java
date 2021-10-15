package run.chronicle.wire.demo.mapreuse;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;

public class Mapper<K, V> extends SelfDescribingMarshallable {

    private final List<V> values = new ArrayList<>();
    private final Function<? super V, ? extends K> extractor;
    private final Comparator<? super K> comparator;

    public Mapper(final Function<? super V, ? extends K> extractor,
                  final Comparator<? super K> comparator) {
        this.extractor = Objects.requireNonNull(extractor);
        this.comparator = Objects.requireNonNull(comparator);
    }

    public List<V> values() {
        return values;
    }

    public Stream<K> keys() {
        return values.stream().map(extractor);
    }

    public void set(Collection<? extends V> values) {
        this.values.clear();
        this.values.addAll(values);
        // Sort the list in id order
        this.values.sort(comparing(extractor, comparator));
    }

    public V get(K key) {
        int index = binarySearch(key);
        if (index >= 0)
            return values.get(index);
        else
            return null;
    }

    int binarySearch(final K key) {
        int low = 0;
        int high = values.size() - 1;

        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final V midVal = values.get(mid);
            int cmp = comparator.compare(
                    extractor.apply(midVal), key);

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