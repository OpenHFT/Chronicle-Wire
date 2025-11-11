/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.nio.ByteBuffer;

/**
 * Provides a bridge to read structured data in Chronicle Wire format from a
 * standard Java {@link InputStream}. The stream is expected to contain a
 * sequence of messages, each prefixed with its length as a 4-byte integer. The
 * payload of each message is read into an internal {@link Bytes} buffer and
 * presented through a {@link Wire} implementation for deserialisation.
 * <p>
 * The provided {@code InputStream} is not closed by this class; callers remain
 * responsible for its lifecycle. This class is useful when Chronicle Wire needs
 * to consume data from sources such as network sockets or files where frames are
 * delimited with explicit length prefixes.
 *
 * @see Wire
 * @see InputStream
 * @see DataInputStream
 */
public class InputStreamToWire {

    /**
     * Internal {@link Bytes} buffer, elastically sized on the heap, used to
     * store the binary data of a single message read from {@link #dis} before it
     * is processed by {@link #wire}.
     */
    private final Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(128);

    /**
     * The {@link Wire} instance that represents the data of the current message
     * according to the {@code wireType} supplied at construction time.
     */
    private final Wire wire;

    /**
     * {@link DataInputStream} wrapping the caller's {@link InputStream} to
     * simplify reading the length and payload bytes.
     */
    private final DataInputStream dis;

    /**
     * Creates a new instance configured with the given wire type and input
     * stream.
     *
     * @param wireType the {@link WireType} used to interpret the message
     *                 payloads
     * @param is       the {@link InputStream} from which data will be read.
     */
    public InputStreamToWire(WireType wireType, InputStream is) {
        wire = wireType.apply(bytes);
        dis = new DataInputStream(is);
    }

    /**
     * Reads the next length-prefixed message from the input stream and makes
     * it available through the internal {@link Wire} instance. The wire and its
     * backing buffer are cleared before reading the 4-byte length header. A
     * negative length results in a {@link StreamCorruptedException}. After the
     * bytes are read, the wire is positioned at the start of the message
     * payload.
     *
     * @return the internal {@link Wire} ready for deserialisation; the same
     *         instance is reused for each call
     * @throws IOException              if an I/O error occurs
     * @throws java.io.EOFException     if the stream ends unexpectedly
     * @throws StreamCorruptedException if a negative length is encountered
     */
    public Wire readOne() throws IOException {
        wire.clear();
        int length = dis.readInt();
        if (length < 0) throw new StreamCorruptedException();
        bytes.ensureCapacity(length);
        byte[] array = bytes.underlyingObject().array();
        dis.readFully(array, 0, length);
        bytes.readPositionRemaining(0, length);
        return wire;
    }
}
