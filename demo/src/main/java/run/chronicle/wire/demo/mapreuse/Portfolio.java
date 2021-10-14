package run.chronicle.wire.demo.mapreuse;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.ToIntFunction;

import static java.util.Comparator.comparingInt;

public class Portfolio extends SelfDescribingMarshallable {

    private int customerId;
    private final List<Security> securities = new ArrayList<>();

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public List<Security> getSecurities() {
        return securities;
    }

    public void setSecurities(Collection<Security> securities) {
        this.securities.clear();
        this.securities.addAll(securities);
        // Sort the list in id order
        this.securities.sort(comparingInt(Security::getId));
    }

    public Security getSecurity(int id) {
        int index = binarySearch(securities, Security::getId, id);
        if (index >= 0)
            return securities.get(index);
        else
            return null;
    }


    public static <T> int binarySearch(final List<T> list,
                                       final ToIntFunction<? super T> extractor,
                                       final int key) {
        int low = 0;
        int high = list.size() - 1;

        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final T midVal = list.get(mid);
            int cmp = Integer.compare(extractor.applyAsInt(midVal), key);

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