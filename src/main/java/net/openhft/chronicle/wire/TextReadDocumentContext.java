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
 * This class represents the context for reading a document in textual format.
 * It provides methods and utilities for understanding the structure and boundaries
 * of the document within a given wire format.
 */
public class TextReadDocumentContext implements ReadDocumentContext {

    // Byte sequences for start and end of the document
    public static final BytesStore<?, ?> SOD_SEP = BytesStore.from("---");
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
     * Consumes the bytes until the end of the message is encountered.
     * Skips bytes that do not denote the end of the document.
     *
     * @param bytes The bytes to be consumed.
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
     * Checks if the current position in the bytes denotes the end of a message.
     * This is done by checking if the bytes start with either the start or end document separator.
     *
     * @param bytes The bytes to be checked.
     * @return True if it's the end of a message; false otherwise.
     */
    public static boolean isEndOfMessage(Bytes<?> bytes) {
        return (bytes.startsWith(SOD_SEP) || bytes.startsWith(EOD_SEP))
                && isWhiteSpaceAt(bytes);
    }

    /**
     * Determines if the byte at a specific position (offset by 3 from current) is a whitespace.
     *
     * @param bytes The bytes to be checked.
     * @return True if the byte is whitespace; false otherwise.
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
     * Skips the document separator sequence (3 bytes) in the given bytes.
     * It also resets the state of the wire's value input and consumes any padding present.
     *
     * @param bytes The bytes in which the separator sequence should be skipped.
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
    public String toString() {
        return Objects.toString(wire);
    }
}
