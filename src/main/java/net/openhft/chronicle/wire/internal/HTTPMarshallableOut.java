package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import static net.openhft.chronicle.bytes.Bytes.*;
import static net.openhft.chronicle.wire.Wires.*;

/**
 * Equivalent to wget --post-data='{data}' http://{host}:{port}/url...
 */

public class HTTPMarshallableOut implements MarshallableOut {
    private final URL url;
    private Wire wire;
    private final DocumentContextHolder dcHolder = new DocumentContextHolder() {
        @Override
        public void close() {
            final boolean chainedElement = chainedElement();
            super.close();
            if (chainedElement)
                return;
            if (wire.bytes().isEmpty())
                return;
            try {
                final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
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


    public HTTPMarshallableOut(MarshallableOutBuilder builder, WireType wireType) {
        this.url = builder.url();

        if (wireType == WireType.JSON)
            this.wire = new JSONWire(allocateElasticOnHeap()).useTypes(true).trimFirstCurly(false).useTextDocuments();
        else if (wireType == WireType.JSON_ONLY) {
            this.wire = new JSONWire(allocateElasticOnHeap()).useTypes(true).useTextDocuments();
        } else
            this.wire = wireType.apply(allocateElasticDirect());

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
        dcHolder.documentContext(wire.acquireWritingDocument(metaData));
        return dcHolder;
    }
}
