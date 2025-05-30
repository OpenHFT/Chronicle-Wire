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
import net.openhft.chronicle.bytes.HexDumpBytes;
import org.jetbrains.annotations.NotNull;

import static net.openhft.chronicle.wire.Wires.toIntU30;

/**
 * A context used for writing documents in a binary format.  Each document is
 * length prefixed so the receiver can skip or buffer whole messages without
 * parsing them first.
 */
public class BinaryWriteDocumentContext implements WriteDocumentContext {

    /** The wire instance used for writing. */
    protected Wire wire;
    /**
     * Write position of the current document header.  The actual length is
     * written back to this location in {@link #close()}.
     */
    protected long position = 0;
    /** Temporary header written at {@code start} containing the NOT_COMPLETE and UNKNOWN_LENGTH flags. */
    protected int tmpHeader;
    /** Number of nested {@code start()} calls. */
    protected int count = 0;
    /** Bit mask representing whether this document is metadata. */
    private int metaDataBit;
    /** True while the document is open. */
    private volatile boolean notComplete;
    /** Indicates that this context belongs to a chain of documents. */
    private boolean chainedElement;
    /** When set, discard bytes written on close. */
    private boolean rollback;

    /**
     * Constructs a new context for writing binary documents using the specified wire.
     *
     * @param wire The wire instance to be used for the writing process.
     */
    public BinaryWriteDocumentContext(Wire wire) {
        this.wire = wire;
    }

    /**
     * Prepares to write a new binary document.  A provisional header containing
     * the {@code NOT_COMPLETE} and {@code UNKNOWN_LENGTH} flags is written and
     * the position is recorded so the correct length can be written on
     * {@link #close()}.  The {@code metaData} argument determines if the
     * metadata bit is set.
     *
     * @param isMetaData true if the document carries metadata rather than user
     *                   data
     */
    public void start(boolean isMetaData) {
        count++;
        // If start() was called more than once, validate the metadata flag.
        if (count > 1) {
            assert isMetaData == isMetaData();
            return;
        }
        @NotNull Bytes<?> bytes = wire().bytes();
        bytes.writePositionForHeader(wire.usePadding());
        bytes.writeHexDumpDescription("msg-length");
        this.position = bytes.writePosition();
        metaDataBit = isMetaData ? Wires.META_DATA : 0;
        tmpHeader = metaDataBit | Wires.NOT_COMPLETE | Wires.UNKNOWN_LENGTH;
        bytes.writeInt(tmpHeader);
        rollback = false;
        notComplete = true;
        chainedElement = false;
    }

    /**
     * Checks whether anything other than the four byte header has been written.
     *
     * @return true if no body bytes have been added yet
     */
    @Override
    public boolean isEmpty() {
        return notComplete && wire().bytes().writePosition() == position + 4;
    }

    @Override
    public boolean isMetaData() {
        return metaDataBit != 0;
    }

    /**
     * Finishes the document.  If this context is not part of a chain and has
     * not been rolled back the body length is calculated and the provisional
     * header at {@link #position} is overwritten with the final length and
     * completion flag.  When rolling back the bytes are cleared instead.
     */
    @Override
    public void close() {
        if (chainedElement)
            return;
        // redundant close
        if (count == 0)
            return;
        count--;
        if (count > 0)
            return;
        notComplete = false;
        @NotNull Bytes<?> bytes = wire().bytes();
        if (rollback) {
            bytes.zeroOut(bytes.readPosition(), bytes.writePosition());
            bytes.writePosition(bytes.readPosition());
            return;
        }

        long position1 = bytes.writePosition();
        long length0 = position1 - position - 4;
        if (length0 > Integer.MAX_VALUE && bytes instanceof HexDumpBytes)
            length0 = (int) length0;
        int length = metaDataBit | toIntU30(length0, "Document length %,d out of 30-bit int range.");
        if (wire.usePadding())
            bytes.testAndSetInt(position, tmpHeader, length);
        else
            bytes.writeInt(position, length);
        wire().getValueOut().resetBetweenDocuments();
    }

    @Override
    public void rollbackIfNotComplete() {
        if (!notComplete) return;
        chainedElement = false;
        count = 1;
        rollback = true;
        close();
    }

    @Override
    public void rollbackOnClose() {
        rollback = true;
    }

    @Override
    public void reset() {
        chainedElement = false;
        if (count > 0)
            close();
        count = 0;
        position = 0;
        metaDataBit = 0;
        tmpHeader = 0;
        rollback = false;
        notComplete = false;
    }

    /**
     * Internal helper used by a few legacy tests.  It clears the
     * {@code notComplete} flag and returns {@code false} so the caller can
     * confirm the context was not left open after a reset.
     */
    // TODO remove asap
    protected boolean checkResetOpened() {
        notComplete = false;
        return false;
    }

    @Override
    public boolean chainedElement() {
        return chainedElement;
    }

    @Override
    public void chainedElement(boolean isChained) {
        this.chainedElement = isChained;
    }

    @Override
    public boolean isPresent() {
        return false;
    }

    @Override
    public Wire wire() {
        return wire;
    }

    /**
     * Returns the byte position where the current document header was written.
     */
    protected long position() {
        return position;
    }

    @Override
    public long index() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int sourceId() {
        return -1;
    }

    @Override
    public boolean isNotComplete() {
        return notComplete;
    }
}
