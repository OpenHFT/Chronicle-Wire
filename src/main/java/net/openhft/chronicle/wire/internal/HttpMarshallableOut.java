package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class HttpMarshallableOut implements MarshallableOut {
    private final URL url;
    private Wire wire;
    private final DocumentContextHolder dcHolder = new DocumentContextHolder() {
        @Override
        public void close() {
            super.close();
            if (wire.bytes().isEmpty())
                return;
            try {
                final URLConnection connection = url.openConnection();
                try {
                    try (final OutputStream out = connection.getOutputStream()) {
                        final Bytes<byte[]> bytes = (Bytes<byte[]>) wire.bytes();
                        out.write(bytes.underlyingObject(), 0, (int) bytes.readLimit());
                    }
                } finally {
                    Closeable.closeQuietly(connection);
                }
            } catch (IOException ioe) {
                throw new IORuntimeException(ioe);
            }
        }
    };

    public HttpMarshallableOut(MarshallableOutBuilder builder) {
        this.url = builder.url();
        this.wire = WireType.JSON.apply(Bytes.allocateElasticOnHeap());
    }

    @Override
    public DocumentContext writingDocument(boolean metaData) throws UnrecoverableTimeoutException {
        dcHolder.documentContext(wire.writingDocument(metaData));
        return dcHolder;
    }

    @Override
    public DocumentContext acquireWritingDocument(boolean metaData) throws UnrecoverableTimeoutException {
        return writingDocument(metaData);
    }
}
