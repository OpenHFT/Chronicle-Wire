package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.Nullable;

public class DocumentContextHolder implements DocumentContext {

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

    public DocumentContext documentContext() {
        return dc;
    }

    public void documentContext(DocumentContext dc) {
        this.dc = dc;
    }

    @Override
    public void close() {
        DocumentContext documentContext = this.dc;
        if (documentContext != null)
            documentContext.close();
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

    public boolean isClosed() {
        return dc == null;
    }
}