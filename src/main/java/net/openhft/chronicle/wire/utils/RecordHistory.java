package net.openhft.chronicle.wire.utils;

import net.openhft.chronicle.wire.VanillaMessageHistory;

/**
 * Functional interface for publishers that prepend a {@link VanillaMessageHistory}
 * before the next message.
 *
 * @param <T> the type of the publishing interface augmented with history recording
 */
public interface RecordHistory<T> {
    /**
     * Records the supplied history so that it is written before the next message.
     *
     * @param history the history to prepend
     * @return typically {@code this} to allow method chaining
     */
    T history(VanillaMessageHistory history);
}
