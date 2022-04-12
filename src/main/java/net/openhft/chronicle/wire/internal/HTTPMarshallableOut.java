package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Equivalent to wget --post-data='{data}' http://{host}:{port}/url...
 */

public class HTTPMarshallableOut implements MarshallableOut {
    private final URL url;
    private Wire wire;
    private final DocumentContextHolder dcHolder = new DocumentContextHolder() {
        @Override
        public void close() {
            if (chainedElement())
                return;
            super.close();
            if (wire.bytes().isEmpty())
                return;
            try {
                final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");

                try {
                    endWire();
                    try (final OutputStream out = conn.getOutputStream()) {
                        final Bytes<byte[]> bytes = (Bytes<byte[]>) wire.bytes();
                        out.write(bytes.underlyingObject(), 0, (int) bytes.readLimit());
                    }

                    final int responseCode = conn.getResponseCode();
                    if (responseCode < 200 || responseCode >= 300)
                        throw new IORuntimeException("ResponseCode: " + responseCode);

                    Closeable.closeQuietly(conn.getInputStream());
                    Closeable.closeQuietly(conn.getErrorStream());
                } finally {
                    Closeable.closeQuietly(conn);
                }
            } catch (IOException ioe) {
                throw new IORuntimeException(ioe);
            }
            startWire();
        }
    };

    public HTTPMarshallableOut(MarshallableOutBuilder builder, WireType wIreType) {
        this.url = builder.url();
        this.wire = wIreType.apply(Bytes.allocateElasticOnHeap());
        startWire();
    }

    void startWire() {
        wire.clear();
        if (wire instanceof JSONWire)
            wire.bytes().append('{').readPosition(1);
    }

    void endWire() {
        if (wire instanceof JSONWire)
            wire.bytes().append('}').append('\n').readPosition(0);
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
