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
 * A context used for writing documents in a binary format.
 * This class provides facilities to start, query, and manage the state of binary
 * documents that are currently being written. The binary format uses headers to
 * denote metadata, data length, and completion status.
 */
public class BinaryWriteDocumentContext implements WriteDocumentContext {

    // The wire instance used for the binary writing process
    protected Wire wire;
    protected long position = 0;
    protected int tmpHeader;
    // Count of how many times the start() method was invoked
    protected int count = 0;
    // Bit representing whether meta data is present
    private int metaDataBit;
    // Flag to indicate if the document write is complete
    private volatile boolean notComplete;
    // Flag to check if the current element is chained
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
     * Initializes the context for starting a new binary write.
     * This will setup necessary headers and markers to facilitate the write.
     *
     * @param metaData A flag indicating whether the write includes metadata.
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

    @Override
    public boolean isEmpty() {
        return notComplete && wire().bytes().writePosition() == position + 4;
    }

    @Override
    public boolean isMetaData() {
        return metaDataBit != 0;
    }

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
     * Retrieves the current position in the wire where the document starts.
     *
     * @return The position in the wire where the current document starts.
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
