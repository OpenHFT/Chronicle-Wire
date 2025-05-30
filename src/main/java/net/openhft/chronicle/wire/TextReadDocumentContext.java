/*
 * Copyright 2016-2025 chronicle.software
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
 * Context for reading a document in textual format. It specifically handles
 * text based wire formats such as YAML or JSON like text, looking for textual
 * separators like "---" and "...". These utilities help understand the
 * structure and boundaries of the document within a given wire format.
 */
public class TextReadDocumentContext implements ReadDocumentContext {

    // Byte sequences for start and end of the document
    /** BytesStore representing the Start-Of-Document separator ('---'). */
    public static final BytesStore<?, ?> SOD_SEP = BytesStore.from("---");
    /** BytesStore representing the End-Of-Document separator ('...'). */
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
     * Static utility method to advance the read position of the supplied
     * {@code Bytes} past the current message until an end of message separator
     * ({@link #SOD_SEP} or {@link #EOD_SEP}) followed by white space is found or
     * the buffer ends.
     *
     * @param bytes the bytes to be consumed
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
     * Static utility to check if the current position in {@code bytes} marks
     * the end of a text message, indicated by {@link #SOD_SEP} or
     * {@link #EOD_SEP} followed by white space.
     *
     * @param bytes the bytes to be checked
     * @return {@code true} if an end of message is detected
     */
    public static boolean isEndOfMessage(Bytes<?> bytes) {
        return (bytes.startsWith(SOD_SEP) || bytes.startsWith(EOD_SEP))
                && isWhiteSpaceAt(bytes);
    }

    /**
     * Static utility to check if the byte three positions after the current
     * read position is a white space character (after "---" or "...").
     *
     * @param bytes the bytes to be checked
     * @return {@code true} if that byte is white space
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
    public void closeReadPosition(long savedReadPosition) {
        this.readPosition = savedReadPosition;
    }

    @Override
    public void closeReadLimit(long savedReadLimit) {
        this.readLimit = savedReadLimit;
    }

    @Nullable
    @Override
    public Wire wire() {
        return wire;
    }

    @Override
    /**
     * Restores the saved {@code readLimit} and {@code readPosition} on the
     * underlying bytes and, unless a rollback was requested, advances past an
     * end of document separator if present. After closing the wire value input
     * state is reset and {@code present} is cleared.
     */
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

    @Override
    /**
     * Locates the start of the next document by consuming padding then skipping
     * any leading separators. It checks {@link #isEndOfMessage(Bytes)} and uses
     * {@link Wire#hasMetaDataPrefix()} to set the metadata flag. The method
     * adjusts the {@code readLimit} to the end of the document and marks this
     * context as present.
     */
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
     * Skips the three byte document separator (for example "---" or "...") in
     * the supplied {@code Bytes}, resets the wire value input state and consumes
     * any following padding.
     *
     * @param bytes the bytes in which the separator should be skipped
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

    @Override
    /**
     * Returns {@link Objects#toString(Object)} of the underlying wire which
     * reveals its current state or content.
     */
    public String toString() {
        return Objects.toString(wire);
    }
}
