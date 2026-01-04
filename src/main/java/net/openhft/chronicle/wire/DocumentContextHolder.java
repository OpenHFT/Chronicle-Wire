/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.Nullable;

/**
 * This is the DocumentContextHolder class which implements both {@link DocumentContext}
 * and {@link WriteDocumentContext}. It acts as a wrapper or a delegate around an instance of
 * {@link DocumentContext}, providing methods to interact with the encapsulated context.
 */
@SuppressWarnings("deprecation")
public class DocumentContextHolder implements DocumentContext, WriteDocumentContext {

    // The encapsulated DocumentContext instance.
    DocumentContext dc;

    /**
     * Creates an empty holder; set a {@link DocumentContext} before use.
     */
    public DocumentContextHolder() {
    }

    @Override
    public boolean isMetaData() {
        return dc.isMetaData();
    }

    @Override
    public boolean isPresent() {
        return dc.isPresent();
    }

    @Override
    public @Nullable
    Wire wire() {
        return dc.wire();
    }

    @Override
    public boolean isNotComplete() {
        return dc.isNotComplete();
    }

    /**
     * Returns the current {@link DocumentContext} held by this wrapper.
     *
     * @return The current {@link DocumentContext} instance.
     */
    public DocumentContext documentContext() {
        return dc;
    }

    /**
     * Sets the encapsulated {@link DocumentContext} instance to the provided value.
     *
     * @param dc The new {@link DocumentContext} to be set.
     */
    public void documentContext(DocumentContext dc) {
        this.dc = dc;
    }

    @Override
    public void close() {
        DocumentContext documentContext = this.dc;
        if (documentContext == null)
            return;
        documentContext.close();
        if (!documentContext.isNotComplete())
            dc = null;
    }

    @Override
    public void reset() {
        DocumentContext documentContext = this.dc;
        if (documentContext != null)
            documentContext.reset();
        this.dc = null;
    }

    @Override
    public int sourceId() {
        return dc.sourceId();
    }

    @Override
    public long index() throws IORuntimeException {
        return dc.index();
    }

    /**
     * Determines if the DocumentContextHolder has been closed or not.
     * This method checks if the encapsulated {@link DocumentContext} is {@code null}, indicating a closed state.
     *
     * @return {@code true} if the holder is closed (i.e., the internal {@link DocumentContext} is {@code null}),
     * {@code false} otherwise.
     */
    public boolean isClosed() {
        return dc == null;
    }

    @Override
    public void start(boolean metaData) {
        ((WriteDocumentContext) dc).start(metaData);
    }

    @Override
    public boolean chainedElement() {
        return ((WriteDocumentContext) dc).chainedElement();
    }

    @Override
    public void chainedElement(boolean chainedElement) {
        ((WriteDocumentContext) dc).chainedElement(chainedElement);
    }

    @Override
    public boolean isEmpty() {
        return ((WriteDocumentContext) dc).isEmpty();
    }
}
