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

import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper around a {@link DocumentContext} implementing both {@link DocumentContext}
 * and {@link WriteDocumentContext}. Useful when behaviour of the underlying context
 * needs to be intercepted or extended without modifying it directly.
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
     * Returns the wrapped {@link DocumentContext}.
     */
    public DocumentContext documentContext() {
        return dc;
    }

    /**
     * Replaces the wrapped {@link DocumentContext}.
     */
    public void documentContext(DocumentContext dc) {
        this.dc = dc;
    }

    /**
     * Delegates to the wrapped context's {@code close} method. If that context
     * reports completion afterwards the internal reference is cleared.
     */
    @Override
    public void close() {
        DocumentContext documentContext = this.dc;
        if (documentContext == null)
            return;
        documentContext.close();
        if (!documentContext.isNotComplete())
            dc = null;
    }

    /**
     * Resets the wrapped context and removes the reference to it.
     */
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
     * Returns {@code true} if there is no wrapped {@link DocumentContext}.
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
