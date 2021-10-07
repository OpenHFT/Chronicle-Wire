package run.chronicle.wire.demo;

import net.openhft.chronicle.core.util.CharSequenceComparator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import static java.util.Comparator.comparing;

/**
 * This example shows how a List can be sorted and used for looking up keys
 * so that no Maps need to be created. The standard Map implementations create
 * a lot of extra objects when values are put into the Map.
 * <p>
 * The sorted List can be reused over and over again.
 */
public class ListLookup {

    public static void main(String[] args) {

        // These can be reused
        final Data d0 = new Data("XYZ", 45, 2);
        final Data d1 = new Data("ABC", 100, 42);
        final Data d2 = new Data("DEF", 200, 13);

        // This can be reused
        final List<Data> dataList = new ArrayList<>();

        dataList.add(d0);
        dataList.add(d1);
        dataList.add(d2);

        // do this once all elements has been added and before marshalling
        dataList.sort(comparing(Data::getId, CharSequenceComparator.INSTANCE));

        // Do this in the application
        int index = binarySearch(dataList, Data::getId, "DEF", CharSequenceComparator.INSTANCE);

        // Use the result
        if (index > 0) {
            System.out.println(dataList.get(index));
        } else {
            System.out.println("Not found!");
        }

    }


    private static final class Data {

        // Ids can be reused/internalized
        private CharSequence id;
        private long price;
        private long count;

        public Data(CharSequence id, long price, long count) {
            this.id = id;
            this.price = price;
            this.count = count;
        }

        public CharSequence getId() {
            return id;
        }

        public void setId(CharSequence id) {
            this.id = id;
        }

        public long getPrice() {
            return price;
        }

        public void setPrice(long price) {
            this.price = price;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

        @Override
        public String toString() {
            return "Data{" +
                    "id=" + id +
                    ", price=" + price +
                    ", count=" + count +
                    '}';
        }
    }


    public static <T, U> int binarySearch(final List<T> list,
                                          final Function<? super T, ? extends U> extractor,
                                          final U key,
                                          final Comparator<U> comparator) {
        int low = 0;
        int high = list.size() - 1;

        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final T midVal = list.get(mid);
            int cmp = comparator.compare(extractor.apply(midVal), key);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found
    }

}
