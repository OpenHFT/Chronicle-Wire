package net.openhft.chronicle.wire;

/**
 * @author Rob Austin.
 */
public interface HeadNumberChecker {
    boolean checkHeaderNumber(long headerNumber, long position);
}
