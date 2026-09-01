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
    //! WireContextListenerLifecycleTest#allConcreteWiresInvokeListenerBeforeFirstDataDocument and
    //! #listenerCannotReenterThroughOuterWire require every writable Wire to share the same
    //! registration, notification and re-entry states. Keeping those transitions behind this
    //! internal seam prevents one Wire type from publishing application data under a different
    //! context lifecycle.
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
    }

    /** Prepares this lifecycle for another output context. */
    default void resetContext() {
    }

    /** Marks a successful context unusable when a later application document is rolled back. */
    default void documentRolledBack() {
    }

    static <T> WireContextListenerLifecycle active(@NotNull Class<T> writerType,
                                                   @NotNull MarshallableOut.ContextListener<? super T> listener) {
        //! WireContextListenerLifecycleTest#listenerFailurePoisonsCurrentContextUntilReset requires
        //! configured outputs to receive private mutable failure state; sharing it would poison or
        //! suppress notifications on an unrelated Wire.
        return new ActiveWireContextListenerLifecycle(requireNonNull(writerType), requireNonNull(listener));
    }
}
