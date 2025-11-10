//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

/**
 * The RollbackIfNotCompleteNotifier interface defines methods to handle rollback
 * scenarios in the event of incomplete operations or messages. Implementations
 * of this interface can ensure data integrity and consistency by rolling back
 * changes if an operation does not complete successfully. This is typically
 * implemented by {@link DocumentContext} objects, particularly
 * {@link WriteDocumentContext}, to allow writers to discard partially written
 * messages.
 */
public interface RollbackIfNotCompleteNotifier {
    /**
     * Rolls back the current operation if it is not complete. If the current
     * document or operation managed by this notifier is in an incomplete state
     * (for example as indicated by {@link DocumentContext#isNotComplete()}), this
     * method should attempt to roll back any changes made within the current
     * context, effectively discarding them. If rollback is not supported or not
     * applicable, an {@link UnsupportedOperationException} may be thrown.
     *
     * @throws UnsupportedOperationException if the rollback operation is not supported
     */
    default void rollbackIfNotComplete() {
        throw new UnsupportedOperationException(getClass().getName());
    }

    /**
     * Checks if the current writing operation is complete. Returns {@code true}
     * by default, indicating completion. Implementations should override this to
     * reflect the actual completion status of their write operation (for
     * example, the inverse of {@link DocumentContext#isNotComplete()}).
     *
     * @return true if the current operation is complete, false otherwise
     */
    default boolean writingIsComplete() {
        return true;
    }
}
