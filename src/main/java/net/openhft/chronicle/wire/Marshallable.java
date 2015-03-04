package net.openhft.chronicle.wire;

/**
 * Created by peter.lawrey on 1/10/15.
 */
public interface Marshallable {
    /**
     * Write data to the wire
     *
     * @param wire to write to.
     */
    public void writeMarshallable(WireOut wire);

    /**
     * Straight line ordered decoding.
     *
     * @param wire to read from in an ordered manner.
     * @throws IllegalStateException the stream wasn't ordered or formatted as expected.
     */
    public void readMarshallable(WireIn wire) throws IllegalStateException;
}
