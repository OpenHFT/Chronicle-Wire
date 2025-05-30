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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * {@link ReadDocumentContext} implementation for textual wire formats such as
 * YAML.  It recognises document separators ("---" and "...") and exposes the
 * current document via the underlying {@link Wire}.
 */
public class TextReadDocumentContext implements ReadDocumentContext {

    /** BytesStore representing the start of a document ('---'). */
    public static final BytesStore<?, ?> SOD_SEP = BytesStore.from("---");
    /** BytesStore representing the end of a document ('...'). */
    public static final BytesStore<?, ?> EOD_SEP = BytesStore.from("...");

    // The wire instance this context operates on
    @Nullable
    protected Wire wire;

    // Indicators for the state of the document
    protected boolean present, notComplete;

    // Metadata flag
    private boolean metaData;

    // Position and limits for reading within the wire
    private long readPosition, readLimit;

    // Starting position (initialized to an invalid position)
    private long start = -1;

    // Rollback flag
    private boolean rollback;

    /**
     * Constructor for the TextReadDocumentContext.
     *
     * @param wire The wire instance to be used by this context. Can be null.
     */
    public TextReadDocumentContext(@Nullable Wire wire) {
        this.wire = wire;
    }

    /**
     * Advance the supplied {@link Bytes} to the end of the current text message
     * looking for a document separator ({@link #SOD_SEP} or {@link #EOD_SEP})
     * followed by whitespace.
     */
    public static void consumeToEndOfMessage(Bytes<?> bytes) {
        while (bytes.readRemaining() > 0) {
            while (bytes.readRemaining() > 0 && bytes.readUnsignedByte() >= ' ') {
                // read skips forward.
            }
            if (isEndOfMessage(bytes)) {
                break;
            }
        }
    }

    /**
     * Test whether the bytes at the current position denote the end of a text
     * message ("---" or "...") followed by whitespace.
     */
    public static boolean isEndOfMessage(Bytes<?> bytes) {
        return (bytes.startsWith(SOD_SEP) || bytes.startsWith(EOD_SEP))
                && isWhiteSpaceAt(bytes);
    }

    /**
     * Returns {@code true} if the byte three positions after the current read
     * position is a whitespace character.
     */
    protected static boolean isWhiteSpaceAt(Bytes<?> bytes) {
        return bytes.peekUnsignedByte(bytes.readPosition() + 3) <= ' ';
    }

    @Override
    public boolean isMetaData() {
        return metaData;
    }

    @Override
    public boolean isPresent() {
        return present;
    }

    @Override
    public void closeReadPosition(long readPosition) {
        this.readPosition = readPosition;
    }

    @Override
    public void closeReadLimit(long readLimit) {
        this.readLimit = readLimit;
    }

    @Nullable
    @Override
    public Wire wire() {
        return wire;
    }

    /**
     * Restore the original read position/limit and skip the trailing document
     * separator if present.
     */
    @Override
    public void close() {
        long readLimit = this.readLimit;
        long readPosition = this.readPosition;

        Wire wire0 = this.wire;
        Bytes<?> bytes = wire0.bytes();
        bytes.readLimit(readLimit);

        if (rollback) {
            if (start > -1)
                bytes.readPosition(start);

            rollback = false;
        } else {
            bytes.readPosition(readPosition);
            if (isEndOfMessage(bytes))
                bytes.readSkip(3);
            while(!bytes.isEmpty()) {
                if (bytes.peekUnsignedByte() > ' ')
                    break;
                bytes.readSkip(1);
            }
        }
        start = -1;

        wire.getValueIn().resetState();
        present = false;
    }

    @Override
    public void reset() {
        close();
        readLimit = 0;
        readPosition = 0;
        start = -1;
        present = false;
        notComplete = false;
        rollback = false;
    }

    /**
     * Locate the next text document, set {@link #isPresent()} accordingly and
     * adjust {@link Bytes#readLimit(long)} so only this document is visible for
     * reading.
     */
    @Override
    public void start() {
        wire.getValueIn().resetState();
        Bytes<?> bytes = wire.bytes();

        present = false;
        wire.consumePadding();
        while(isEndOfMessage(bytes))
            skipSep(bytes);

        if (bytes.readRemaining() < 1) {
            readLimit = readPosition = bytes.readLimit();
            notComplete = false;
            return;
        }

        metaData = wire.hasMetaDataPrefix();
        start = bytes.readPosition();
        consumeToEndOfMessage(bytes);

        readLimit = bytes.readLimit();
        readPosition = bytes.readPosition();

        bytes.readLimit(bytes.readPosition());
        bytes.readPosition(start);
        present = true;
    }

    /**
     * Skip the 3-byte document separator in the supplied bytes and consume any
     * following padding.
     */
    protected void skipSep(Bytes<?> bytes) {
        // Skip 3 bytes (length of the separator sequence)
        bytes.readSkip(3);

        // Reset the state of the value input in the wire
        wire.getValueIn().resetState();

        // Consume any padding present in the wire
        wire.consumePadding();
    }

    @Override
    public void rollbackOnClose() {
        rollback = true;
    }

    @Override
    public long index() {
        return readPosition;
    }

    @Override
    public int sourceId() {
        return -1;
    }

    @Override
    public boolean isNotComplete() {
        return notComplete;
    }

    /**
     * Delegates to {@code Objects.toString(wire)} so the underlying wire's
     * textual representation is returned.
     */
    @Override
    public String toString() {
        return Objects.toString(wire);
    }
}
