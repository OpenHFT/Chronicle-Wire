/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

//! DocumentContextLifecycleTest#noDocumentContextReturnsNegativeContextCount demonstrates that the
//! concrete NOOP retains the IgnoresEverything marker expected by existing fast-path checks.
import net.openhft.chronicle.core.util.IgnoresEverything;

/**
 * An enumeration implementation of the {@link DocumentContext} interface.
 * This context represents a non-existent or uninitialized document context,
 * hence all its methods return default or null values.
 *
 * <p>This can be useful as a sentinel value or placeholder to avoid null checks in code
 * that works with document contexts. Using `NoDocumentContext.INSTANCE` denotes
 * a guaranteed uninitialized state for a document context.
 */
//! DocumentContextLifecycleTest#noDocumentContextReturnsNegativeContextCount demonstrates that the
//! sentinel remains IgnoresEverything-compatible and harmless under rollback.
public enum NoDocumentContext implements DocumentContext, IgnoresEverything {
    /** The singleton instance of the NoDocumentContext */
    INSTANCE;

    @Override
    public boolean isMetaData() {
        return false;
    }

    @Override
    public boolean isPresent() {
        return false;
    }

    @Override
    public boolean isData() {
        return false;
    }

    @Override
    public Wire wire() {
        return null;
    }

    @Override
    public int sourceId() {
        return -1;
    }

    @Override
    public long index() {
        return Long.MIN_VALUE;
    }

    @Override
    public int contextCount() {
        //! DocumentContextLifecycleTest#noDocumentContextReturnsNegativeContextCount demonstrates that
        //! no document must not resemble writable context zero.
        return -1;
    }

    @Override
    public boolean isNotComplete() {
        return false;
    }

    @Override
    public void close() {
        // Do nothing
    }

    @Override
    public void rollbackIfNotComplete() {
        //! DocumentContextLifecycleTest#noDocumentContextReturnsNegativeContextCount demonstrates that
        //! rollback on the public NOOP sentinel remains harmless.
    }

    @Override
    public void reset() {
        close();
    }
}
