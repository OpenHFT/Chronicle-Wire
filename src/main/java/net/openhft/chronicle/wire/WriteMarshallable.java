package net.openhft.chronicle.wire;

public interface WriteMarshallable {
    /**
     * Write data to the wire
     *
     * @param wire to write to.
     */
    void writeMarshallable(WireOut wire);
}
