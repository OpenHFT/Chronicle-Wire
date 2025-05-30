/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

/**
 * An enumeration implementation of {@link DocumentContext} representing an
 * absent or uninitialised document. All methods return neutral values.
 *
 * <p>This is useful as a sentinel to avoid {@code null} checks in code that
 * works with document contexts. Using {@code NoDocumentContext.INSTANCE}
 * denotes a guaranteed uninitialised state.
 */
public enum NoDocumentContext implements DocumentContext {
    /** The singleton instance of the {@code NoDocumentContext}. */
    INSTANCE;

    /**
     * Returns {@code false} as this context has no metadata section.
     */
    @Override
    public boolean isMetaData() {
        return false;
    }

    /**
     * Always returns {@code false} because no document is present.
     */
    @Override
    public boolean isPresent() {
        return false;
    }

    /**
     * Always returns {@code false} as this is not a data document.
     */
    @Override
    public boolean isData() {
        return false;
    }

    /**
     * Returns {@code null} because there is no associated {@link Wire}.
     */
    @Override
    public Wire wire() {
        return null;
    }

    /**
     * Returns {@code -1} to indicate the absence of a source identifier.
     */
    @Override
    public int sourceId() {
        return -1;
    }

    /**
     * Returns {@link Long#MIN_VALUE} as no index exists.
     */
    @Override
    public long index() {
        return Long.MIN_VALUE;
    }

    /**
     * Always returns {@code false}; there is nothing to complete.
     */
    @Override
    public boolean isNotComplete() {
        return false;
    }

    /**
     * Does nothing as there is no underlying resource to close.
     */
    @Override
    public void close() {
        // Do nothing
    }

    /**
     * Resets the context by invoking {@link #close()}.
     */
    @Override
    public void reset() {
        close();
    }
}
