package net.openhft.chronicle.wire;

/**
 * Created by peter.lawrey on 02/02/2016.
 */
class ValueInState {

    private static final long[] EMPTY_ARRAY = {};
    private long savedPosition;
    private int unexpectedSize;
    private long[] unexpected = EMPTY_ARRAY;

    public void reset() {
        savedPosition = 0;
        unexpectedSize = 0;
    }

    public void addUnexpected(long position) {
        if (unexpectedSize >= unexpected.length) {
            int newSize = unexpected.length * 3 / 2 + 8;
            long[] unexpected2 = new long[newSize];
            System.arraycopy(unexpected, 0, unexpected2, 0, unexpected.length);
            unexpected = unexpected2;
        }
        unexpected[unexpectedSize++] = position;
    }

    public void savedPosition(long savedPosition) {
        this.savedPosition = savedPosition;
    }

    public long savedPosition() {
        return savedPosition;
    }

    public int unexpectedSize() {
        return unexpectedSize;
    }

    public long unexpected(int index) {
        return unexpected[index];
    }

    public void removeUnexpected(int i) {
        int length = unexpectedSize - i - 1;
        if (length > 0)
            System.arraycopy(unexpected, i + 1, unexpected, i, length);
        unexpectedSize--;
    }
}
