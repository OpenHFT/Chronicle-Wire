package net.openhft.chronicle.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created by peter on 16/01/15.
 */
public class FastStack<T> {
    private final Supplier<T> tSupplier;
    private int top = -1;
    private final List<T> tList = new ArrayList<>();

    public FastStack(Supplier<T> tSupplier) {
        this.tSupplier = tSupplier;
    }

    public T push() {
        if (++top >= tList.size())
            tList.add(tSupplier.get());
        return tList.get(top);
    }

    public T pop() {
        if (top < 0)
            throw new IllegalStateException();
        return tList.get(top--);
    }

    public T peek() {
        return tList.get(top);
    }
}
