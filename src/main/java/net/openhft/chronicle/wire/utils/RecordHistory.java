package net.openhft.chronicle.wire.utils;

/**
 * Write a history before each message
 *
 * @param <T> interface of messages with prepended history
 */
@SuppressWarnings("deprecation")
public interface RecordHistory<T> {
    T history(net.openhft.chronicle.wire.VanillaMessageHistory history);
}
