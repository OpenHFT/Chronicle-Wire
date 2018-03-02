package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;

/**
 * Created by Rob Austin - see usage in chronicle services
 */
public interface FieldNumberParselet<O> {
    void readOne(long methodId, Bytes bytes, O o);
}
