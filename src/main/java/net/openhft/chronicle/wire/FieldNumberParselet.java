package net.openhft.chronicle.wire;

/**
 * Created by Rob Austin - see usage in chronicle services
 */
public interface FieldNumberParselet<O> {
    void readOne(long methodId, WireIn wire, O o);
}
