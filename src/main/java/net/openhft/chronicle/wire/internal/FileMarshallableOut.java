package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

public class FileMarshallableOut implements MarshallableOut {
    private final URL url;
    private Wire wire;
    private final DocumentContextHolder dcHolder = new DocumentContextHolder() {
        @Override
        public void close() {
            super.close();
            if (wire.bytes().isEmpty())
                return;
            try (FileOutputStream out = new FileOutputStream(url.getFile())) {
                final Bytes<byte[]> bytes = (Bytes<byte[]>) wire.bytes();
                out.write(bytes.underlyingObject(), 0, (int) bytes.readLimit());
            } catch (IOException ioe) {
                throw new IORuntimeException(ioe);
            }
        }
    };

    public FileMarshallableOut(MarshallableOutBuilder builder, WireType wIreType) {
        this.url = builder.url();
        assert url.getProtocol().equals("file");
        this.wire = wIreType.apply(Bytes.allocateElasticOnHeap());
    }

    @Override
    public DocumentContext writingDocument(boolean metaData) throws UnrecoverableTimeoutException {
        dcHolder.documentContext(wire.writingDocument(metaData));
        return dcHolder;
    }

    @Override
    public DocumentContext acquireWritingDocument(boolean metaData) throws UnrecoverableTimeoutException {
        dcHolder.documentContext(wire.acquireWritingDocument(metaData));
        return dcHolder;
    }
}
