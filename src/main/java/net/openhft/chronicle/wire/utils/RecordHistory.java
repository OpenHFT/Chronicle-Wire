package net.openhft.chronicle.wire.utils;

import net.openhft.chronicle.wire.VanillaMessageHistory;

/**
 * Write a history before each message
 *
 * @param <T> interface of messages with prepended history
 */
public interface RecordHistory<T> {
    T history(VanillaMessageHistory history);
}
