package net.openhft.chronicle.wire;

public interface Sequence {

    /**
     * returns the sequence or Long.MIN_VALUE if the sequence can not be resolved
     */
    long sequence();

    void sequence(long sequence, long position);
}
