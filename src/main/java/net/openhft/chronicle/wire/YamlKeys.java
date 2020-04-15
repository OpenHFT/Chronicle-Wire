package net.openhft.chronicle.wire;

import java.util.Arrays;

public class YamlKeys {
    private static final long[] NO_OFFSETS = {};

    int count = 0;
    long[] offsets = NO_OFFSETS;

    public void push(long offset) {
        if (count == offsets.length) {
            int size = Math.max(7, offsets.length * 2);
            offsets = Arrays.copyOf(offsets, size);
        }
        offsets[count++] = offset;
    }

    public int count() {
        return count;
    }

    public long[] offsets() {
        return offsets;
    }

    public void reset() {
        count = 0;
    }

    public void removeIndex(int i) {
        count--;
        int length = count - i;
        if (length > 0)
            System.arraycopy(offsets, i + 1, offsets, i, length);
    }
}
