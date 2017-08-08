package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.util.StringUtils;

public class CharSequenceObjectMap<T> {
    private static final int K0 = 0x6d0f27bd;
    private static final int M0 = 0x5bc80bad;

    final String[] keys;
    final T[] values;
    final int mask;

    public CharSequenceObjectMap(int capacity) {
        int nextPower2 = Maths.nextPower2(capacity, 16);
        keys = new String[nextPower2];
        values = (T[]) new Object[nextPower2];
        mask = nextPower2 - 1;
    }

    public void put(CharSequence name, T t) {
        int h = hashFor(name);
        for (int i = 0; i < mask; i++) {
            if (keys[i] == null || StringUtils.isEqual(keys[i], name)) {
                keys[i] = name.toString();
                values[i] = t;
                return;
            }
            h = (h + 1) & mask;
        }
        throw new IllegalStateException("Map is full");
    }

    public T get(CharSequence cs) {
        int h = hashFor(cs);
        for (int i = 0; i < mask; i++) {
            if (keys[i] == null)
                return null;
            if (StringUtils.isEqual(keys[i], cs))
                return values[i];
            h = (h + 1) & mask;
        }
        throw new IllegalStateException("Map is full");
    }

    private int hashFor(CharSequence name) {
        long h = name.length();
        for (int i = 0; i < name.length(); i++) {
            h = h * K0 + name.charAt(i);
        }
        return (int) Maths.agitate(h) & mask;
    }
}
