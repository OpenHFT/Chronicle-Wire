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

package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static net.openhft.chronicle.bytes.Bytes.allocateElasticOnHeap;

/**
 * Equivalent to wget --post-data='{data}' http://{host}:{port}/url...
 */

public class HTTPMarshallableOut implements MarshallableOut {
    private final URL url;
    private final Wire wire;
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
        }
    };


    public HTTPMarshallableOut(MarshallableOutBuilder builder, WireType wireType) {
        this.url = builder.url();

        if (wireType == WireType.JSON)
            this.wire = new JSONWire(allocateElasticOnHeap()).useTypes(true).trimFirstCurly(true).useTextDocuments();
        else
            this.wire = wireType.apply(allocateElasticOnHeap());

        startWire();
    }

    void startWire() {
        wire.clear();
    }

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
}
