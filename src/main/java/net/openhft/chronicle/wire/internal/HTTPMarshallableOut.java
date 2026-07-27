/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static net.openhft.chronicle.bytes.Bytes.allocateElasticOnHeap;

/**
 * This class allows for the serialization of {@link Marshallable} objects and their transmission over HTTP using the POST method.
 * It is conceptually similar to the command {@code wget --post-data='{data}' http://{host}:{port}/url...}.
 * <p>
 * The class encapsulates a {@link Wire} which holds the serialized representation. On closure of a document context,
 * the serialized content is posted to the given URL.
 */
@SuppressWarnings("this-escape")
public class HTTPMarshallableOut implements MarshallableOut {

    // The target URL to which serialized data is posted
    private final URL url;

    // The encapsulated Wire object for serialization
    private final Wire wire;

    // One-based count of the POST currently being built: each document is delivered over its own
    // HTTP connection, so each is a distinct output context for DocumentContext.contextCount().
    private long connectionCount = 1;

    // Document context holder for managing the wire and the HTTP communication
    private final DocumentContextHolder dcHolder = new DocumentContextHolder() {

        @Override
        public long contextCount() {
            return connectionCount;
        }

        // Inline comment about override functionality
        @Override
        public void close() {
            // Logic for managing wire and HTTP communication
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
                        final Bytes<byte[]> bytes = Jvm.uncheckedCast(wire.bytes());
                        final byte[] b = bytes.underlyingObject();
                        assert b != null;
                        out.write(b, 0, (int) bytes.readLimit());
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
            connectionCount++; // the next document goes over a new connection
        }
    };

    /**
     * Constructs an HTTPMarshallableOut object with the provided builder and wire type.
     *
     * @param builder   The {@link MarshallableOutBuilder} providing configuration details.
     * @param wireType  The type of Wire for serialization.
     */
    public HTTPMarshallableOut(MarshallableOutBuilder builder, WireType wireType) {
        this.url = builder.url();

        if (wireType == WireType.JSON)
            this.wire = new JSONWire(allocateElasticOnHeap()).useTypes(true).trimFirstCurly(true).useTextDocuments();
        else
            this.wire = wireType.apply(allocateElasticOnHeap());

        startWire();
    }

    // Method for resetting the wire state
    void startWire() {
        wire.clear();
    }

    // Method for finalizing the wire content
    void endWire() {
        if (!wire.isBinary()) {
            final Bytes<?> bytes = wire.bytes();
            if (bytes.peekUnsignedByte(bytes.writePosition() - 1) >= ' ')
                bytes.append('\n');
        }
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
    @Override
    public <T> MarshallableOut contextListener(Class<T> writerType,
                                               MarshallableOut.ContextListener<? super T> listener) {
        throw new UnsupportedOperationException(
                "contextListener is not supported for HTTP output: each POST is a new output context");
    }

}
