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
 * Wraps another {@link DocumentContext} and delegates all operations to it.
 * Sub-classes extend this base to add specialised behaviour.
 */
public abstract class WrappedDocumentContext implements DocumentContext {

    // The wrapped instance of DocumentContext.
    private DocumentContext dc;

    /**
     * Constructs a new instance of WrappedDocumentContext that wraps the provided DocumentContext.
     *
     * @param dc The DocumentContext to be wrapped.
     */
    protected WrappedDocumentContext(DocumentContext dc) {
        this.dc = dc;
    }

    /**
     * Returns the currently wrapped {@link DocumentContext}.
     */
    public DocumentContext dc() {
        return dc;
    }

    /**
     * Sets or replaces the {@link DocumentContext} being wrapped and returns this instance.
     */
    public WrappedDocumentContext dc(DocumentContext dc) {
        this.dc = dc;
        return this;
    }

    /**
     * Delegates to {@link DocumentContext#isMetaData()} on the wrapped instance.
     */
    @Override
    public boolean isMetaData() {
        return dc.isMetaData();
    }

    /**
     * Delegates to {@link DocumentContext#isPresent()} on the wrapped instance.
     */
    @Override
    public boolean isPresent() {
        return dc.isPresent();
    }

    /**
     * Delegates to {@link DocumentContext#wire()} on the wrapped instance.
     */
    @Nullable
    @Override
    public Wire wire() {
        return dc.wire();
    }

    /**
     * Delegates to {@link DocumentContext#isNotComplete()} on the wrapped instance.
     */
    @Override
    public boolean isNotComplete() {
        return dc.isNotComplete();
    }

    /**
     * Delegates to {@link DocumentContext#close()} on the wrapped instance.
     */
    @Override
    public void close() {
        dc.close();
    }

    /**
     * Delegates to {@link DocumentContext#sourceId()} on the wrapped instance.
     */
    @Override
    public int sourceId() {
        return dc.sourceId();
    }

    /**
     * Delegates to {@link DocumentContext#index()} on the wrapped instance.
     */
    @Override
    public long index() throws IORuntimeException {
        return dc.index();
    }

    /**
     * Delegates to {@link DocumentContext#isData()} on the wrapped instance.
     */
    @Override
    public boolean isData() {
        return dc.isData();
    }

    /**
     * Delegates to {@link DocumentContext#rollbackOnClose()} on the wrapped instance.
     */
    @Override
    public void rollbackOnClose() {
        dc.rollbackOnClose();
    }
}
