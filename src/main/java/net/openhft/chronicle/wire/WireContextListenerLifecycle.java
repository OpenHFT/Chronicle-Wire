/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Internal lifecycle for a context listener attached to a wire.
 *
 * <p>The shared {@link NoOpWireContextListenerLifecycle#SET} instance represents a wire whose first
 * document has started without a listener. An {@link ActiveWireContextListenerLifecycle} is
 * allocated only when the advanced context-listener feature is configured.</p>
 */
interface WireContextListenerLifecycle {
    /**
     * @return whether an output document has already started
     */
    boolean started();

    /**
     * Invoked immediately before a wire starts an output document.
     *
     * @param wire     wire starting the document
     * @param metaData whether the document contains metadata
     */
    void beforeDocument(AbstractWire wire, boolean metaData);

    /** Verifies that resetting the output context cannot interrupt notification. */
    default void checkCanResetContext() {
        /// WireContextListenerLifecycleTest#listenerCannotClearTheOuterWire demonstrates the active
        /// override; listener-free implementations deliberately need no additional work here.
    }

    /** Prepares this lifecycle for another output context. */
    default void resetContext() {
        /// WireContextListenerLifecycleTest#resetReusesListenerForTheNextOutputContext demonstrates the
        /// active override; listener-free contexts retain their allocation-free default.
    }

    /** Marks a successful context unusable when a later application document is rolled back. */
    default void documentRolledBack() {
        /// WireContextListenerLifecycleTest#applicationSerializationFailurePoisonsSuccessfulContextUntilReset
        /// demonstrates the active override; without a listener there is no context record to poison.
    }

    static <T> WireContextListenerLifecycle active(@NotNull Class<T> writerType,
                                                   @NotNull MarshallableOut.ContextListener<? super T> listener) {
        /// WireContextListenerLifecycleTest#contextListenerWaitsForDataAfterMetadataAndWritesDtoOnce
        /// demonstrates that registration creates per-Wire state while retaining the supplied types.
        return new ActiveWireContextListenerLifecycle(requireNonNull(writerType), requireNonNull(listener));
    }
}
