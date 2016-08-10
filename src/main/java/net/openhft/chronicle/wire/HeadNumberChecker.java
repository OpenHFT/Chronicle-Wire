package net.openhft.chronicle.wire;

/**
 * @author Rob Austin.
 */
public interface HeadNumberChecker {
    void checkHeaderNumber(long headerNumber, long position);
}
