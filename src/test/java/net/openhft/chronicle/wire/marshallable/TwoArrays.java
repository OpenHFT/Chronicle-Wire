package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.ref.BinaryIntArrayReference;
import net.openhft.chronicle.bytes.ref.BinaryLongArrayReference;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.values.IntArrayValues;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;

public class TwoArrays extends SelfDescribingMarshallable implements Closeable {
    IntArrayValues ia;
    LongArrayValues la;
    transient boolean closed;

    public TwoArrays(int isize, long lsize) {
        this.ia = new BinaryIntArrayReference(isize);
        this.la = new BinaryLongArrayReference(lsize);
    }

    @Override
    public void close() {
        Closeable.closeQuietly(ia, la);
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}
