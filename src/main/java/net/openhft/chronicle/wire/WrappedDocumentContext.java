/*
 * Copyright 2016-2020 chronicle.software
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
 * An abstract class that wraps around an instance of DocumentContext.
 * It delegates the methods of the DocumentContext interface to the wrapped instance.
 */
public abstract class WrappedDocumentContext implements DocumentContext {
    private DocumentContext dc;

    /**
     * Constructs a WrappedDocumentContext object with a given DocumentContext.
     *
     * @param dc the DocumentContext to wrap
     */
    protected WrappedDocumentContext(DocumentContext dc) {
        this.dc = dc;
    }

    /**
     * Returns the wrapped DocumentContext.
     *
     * @return the wrapped DocumentContext
     */
    public DocumentContext dc() {
        return dc;
    }

    /**
     * Sets the wrapped DocumentContext to the provided DocumentContext.
     *
     * @param dc the DocumentContext to wrap
     * @return this WrappedDocumentContext
     */
    public WrappedDocumentContext dc(DocumentContext dc) {
        this.dc = dc;
        return this;
    }

    @Override
    public boolean isMetaData() {
        return dc.isMetaData();
    }

    @Override
    public boolean isPresent() {
       return dc.isPresent();
    }

    @Nullable
    @Override
    public Wire wire() {
        return dc.wire();
    }

    @Override
    public boolean isNotComplete() {
        return dc.isNotComplete();
    }

    @Override
    public void close() {
        dc.close();
    }

    @Override
    public int sourceId() {
        return dc.sourceId();
    }

    @Override
    public long index() throws IORuntimeException {
        return dc.index();
    }

    @Override
    public boolean isData() {
        return dc.isData();
    }

    @Override
    public void rollbackOnClose() {
        dc.rollbackOnClose();
    }
}
