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
 * DocumentContextHolder is a concrete class that implements both DocumentContext and
 * WriteDocumentContext interfaces. This class is used to encapsulate a DocumentContext and
 * provide additional write functionality, allowing it to interact with the encapsulated
 * DocumentContext and manage its state.
 */
public class DocumentContextHolder implements DocumentContext, WriteDocumentContext {

    // The encapsulated DocumentContext
    private DocumentContext dc;

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
     * Retrieves the encapsulated DocumentContext.
     *
     * @return The encapsulated DocumentContext.
     */
    public DocumentContext documentContext() {
        return dc;
    }

    /**
     * Sets the encapsulated DocumentContext.
     *
     * @param dc The new DocumentContext to encapsulate.
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
     * Checks if the encapsulated DocumentContext is closed.
     *
     * @return {@code true} if the encapsulated DocumentContext is closed,
     *         otherwise {@code false}.
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