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
import net.openhft.chronicle.bytes.HexDumpBytes;
import org.jetbrains.annotations.NotNull;

import static net.openhft.chronicle.wire.Wires.toIntU30;

/**
 * {@link WriteDocumentContext} for binary length prefixed messages.
 */
public class BinaryWriteDocumentContext implements WriteDocumentContext {

    /** Wire the document is written to. */
    protected Wire wire;
    protected long position = 0;
    protected int tmpHeader;
    /** Nesting depth of {@link #start(boolean)} calls. */
    protected int count = 0;
    /** Bit mask for metadata in the header. */
    private int metaDataBit;
    /** True while the document is still open. */
    private volatile boolean notComplete;
    /** Part of a chained write. */
    private boolean chainedElement;
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
     * Begin writing a new binary document by writing a placeholder header.
     */
    public void start(boolean metaData) {
        count++;
        // If start() was called more than once, validate the metadata flag.
        if (count > 1) {
            assert metaData == isMetaData();
            return;
        }
        @NotNull Bytes<?> bytes = wire().bytes();
        bytes.writePositionForHeader(wire.usePadding());
        bytes.writeHexDumpDescription("msg-length");
        this.position = bytes.writePosition();
        metaDataBit = metaData ? Wires.META_DATA : 0;
        tmpHeader = metaDataBit | Wires.NOT_COMPLETE | Wires.UNKNOWN_LENGTH;
        bytes.writeInt(tmpHeader);
        rollback = false;
        notComplete = true;
        chainedElement = false;
    }

    /**
     * Returns {@code true} if no data beyond the 4 byte header has been
     * written.
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
     * Finalise the header with the actual length unless this context was rolled
     * back.
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

    /**
     * Roll back the current document if it has not been completed yet.
     */
    @Override
    public void rollbackIfNotComplete() {
        if (!notComplete) return;
        chainedElement = false;
        count = 1;
        rollback = true;
        close();
    }

    /**
     * Mark the document for rollback when {@link #close()} is invoked.
     */
    @Override
    public void rollbackOnClose() {
        rollback = true;
    }

    /**
     * Reset all state so the context may be reused.
     */
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
    public void chainedElement(boolean chainedElement) {
        this.chainedElement = chainedElement;
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
     * Position where the header for the current document was written.
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
