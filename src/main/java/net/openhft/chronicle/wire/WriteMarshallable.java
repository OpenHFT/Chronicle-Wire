package net.openhft.chronicle.wire;

@FunctionalInterface
public interface WriteMarshallable {
    /**
     * Write data to the wire
     *
     * @param wire to write to.
     */
    void writeMarshallable(WireOut wire);
}
