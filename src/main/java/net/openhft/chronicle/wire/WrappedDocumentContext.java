/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.Nullable;

public abstract class WrappedDocumentContext implements DocumentContext {
    private DocumentContext dc;

    protected WrappedDocumentContext(DocumentContext dc) {
        this.dc = dc;
    }

    public DocumentContext dc() {
        return dc;
    }

    public WrappedDocumentContext dc(DocumentContext dc) {
        this.dc = dc;
        return this;
    }

    @Override
    public boolean isMetaData() {
        return dc.isMetaData();
    }

    @Override
    public void metaData(boolean metaData) {
         dc.metaData(metaData);
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

    @Override
    public void notifyClosing() {
        dc.notifyClosing();
    }

    @Override
    public boolean isClosed() {
        return dc.isClosed();
    }
}
