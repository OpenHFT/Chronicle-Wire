/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
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

import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.Nullable;

/**
 * This is the DocumentContextHolder class which implements both {@link DocumentContext}
 * and {@link WriteDocumentContext}. It acts as a wrapper or a delegate around an instance of
 * {@link DocumentContext}, providing methods to interact with the encapsulated context.
 *
 * @since 2023-09-14
 */
public class DocumentContextHolder implements DocumentContext, WriteDocumentContext {

    // The encapsulated DocumentContext instance.
    DocumentContext dc;

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
     * Retrieves the encapsulated {@link DocumentContext} instance.
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
