package net.openhft.chronicle.wire;

@FunctionalInterface
public interface ReadMarshallable {
    /**
     * Straight line ordered decoding.
     *
     * @param wire to read from in an ordered manner.
     * @throws IllegalStateException the stream wasn't ordered or formatted as expected.
     */
    void readMarshallable(WireIn wire) throws IllegalStateException;
}
