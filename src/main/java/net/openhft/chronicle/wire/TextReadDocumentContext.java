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
 * Implementation of the ReadDocumentContext interface for reading text data from a document.
 */
public class TextReadDocumentContext implements ReadDocumentContext {
    public static final BytesStore<?, ?> SOD_SEP = BytesStore.from("---");
    public static final BytesStore<?, ?> EOD_SEP = BytesStore.from("...");
    @Nullable
    protected AbstractWire wire;
    protected boolean present, notComplete;

    private boolean metaData;
    private long readPosition, readLimit;
    private long start = -1;
    private boolean rollback;

    /**
     * Constructor of TextReadDocumentContext that initializes the context with an instance of AbstractWire.
     *
     * @param wire Instance of AbstractWire.
     */
    public TextReadDocumentContext(@Nullable AbstractWire wire) {
        this.wire = wire;
    }


    /**
     * Consumes to the end of the message in the provided Bytes object.
     *
     * @param bytes Bytes object to be consumed.
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

    public static boolean isEndOfMessage(Bytes<?> bytes) {
        return (bytes.startsWith(SOD_SEP) || bytes.startsWith(EOD_SEP))
                && isWhiteSpaceAt(bytes);
    }

    protected static boolean isWhiteSpaceAt(Bytes<?> bytes) {
        return bytes.peekUnsignedByte(bytes.readPosition() + 3) <= ' ';
    }


    /**
     * Returns if the context has metadata available for reading.
     *
     * @return boolean value indicating if metadata is present.
     */
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
     * Closes the current context. Updates the read limits and positions. Rolls back if rollback flag is set.
     */
    @Override
    public void close() {
        long readLimit = this.readLimit;
        long readPosition = this.readPosition;

        AbstractWire wire0 = this.wire;
        wire0.bytes.readLimit(readLimit);

        if (rollback) {
            if (start > -1)
                wire0.bytes.readPosition(start);

            rollback = false;
        } else {
            wire0.bytes.readPosition(readPosition);
        }
        start = -1;

        wire.getValueIn().resetState();
        present = false;
    }

    /**
     * Resets the current context, clearing any reading operations or flags that have been set.
     */
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
     * Starts the process of reading data from the document.
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

    protected void skipSep(Bytes<?> bytes) {
        bytes.readSkip(3);
        wire.getValueIn().resetState();
        wire.consumePadding();
    }

    /**
     * Sets the rollback flag to true. It means that the document context will be rolled back to
     * the state before opening when it is closed.
     */
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
