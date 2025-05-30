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
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.scoped.ScopedResource;
import net.openhft.chronicle.core.scoped.ScopedResourcePool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.openhft.chronicle.wire.Wires.lengthOf;

/**
 * Concrete {@link ReadDocumentContext} for reading length-prefixed messages from a
 * {@link Wire}. It can roll back to the document start if required and was previously able
 * to ensure a full read for delta-encoded wires.
 */
public class BinaryReadDocumentContext implements ReadDocumentContext {

    /**
     * Stores the read position at the beginning of the current document.
     * Used for rollbacks and diagnostics.
     */
    public long start = -1;
    /**
     * Remembers the previous start position for debugging support.
     */
    public long lastStart = -1;
    /**
     * Wire from which the document is read.
     */
    @Nullable
    protected Wire wire;
    /**
     * True when a document is available to read.
     */
    protected boolean present;
    /**
     * Indicates the header flagged NOT_COMPLETE or end of data.
     */
    protected boolean notComplete;
    /**
     * Remembered read position to restore on close.
     */
    protected long readPosition;
    /**
     * Limit of the bytes belonging to this message.
     */
    protected long readLimit;
    /**
     * True if the message was metadata rather than user data.
     */
    protected boolean metaData;
    /**
     * Marker that {@link #rollbackOnClose()} was requested.
     */
    protected boolean rollback;

    /**
     * Create a reader for the supplied wire.
     *
     * @param wire wire to read from
     */
    public BinaryReadDocumentContext(@Nullable Wire wire) {
        this.wire = wire;
    }

    /**
     * Deprecated constructor kept for binary compatibility. The boolean once
     * signalled that delta encoded wires required a full read, but DeltaWire is
     * no longer supported.
     *
     * @param wire           wire to read from
     * @param ensureFullRead ignored
     */
    @Deprecated(/* to be removed in x.29 */)
    public BinaryReadDocumentContext(@Nullable Wire wire, boolean ensureFullRead) {
        this.wire = wire;
        assert !ensureFullRead : "DeltaWire not supported";
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
     * Determines if a rollback is required for this document context.
     *
     * @return {@code true} if rollback is required, {@code false} otherwise.
     */
    protected boolean rollback() {
        return rollback;
    }

    static final ScopedResourcePool<StringBuilder> SBP = StringBuilderPool.createThreadLocal(1);

    @Override
    public void close() {
        if (rollbackIfNeeded())
            return;

        long readLimit0 = this.readLimit;
        long readPosition0 = this.readPosition;

        Wire wire0 = this.wire;
        start = -1;
        if (readLimit0 > 0 && wire0 != null) {
            @NotNull final Bytes<?> bytes = wire0.bytes();
            bytes.readLimit(readLimit0);
            if (wire.usePadding())
                readPosition0 += BytesUtil.padOffset(readPosition0);
            bytes.readPosition(Math.min(readLimit0, readPosition0));
        }

        present = false;
    }

    @Override
    public void reset() {
        close();
        readLimit = readPosition = 0;
        lastStart = start = -1;
    }

    /**
     * If {@link #rollbackOnClose()} was invoked, reset the bytes to the start of
     * the current document and clear the rollback marker.
     *
     * @return {@code true} when a rollback occurred
     */
    protected boolean rollbackIfNeeded() {
        if (rollback) {
            present = false;
            rollback = false;
            if (start > -1)
                wire.bytes().readPosition(start).readLimit(readLimit);
            start = -1;
            return true;
        }

        return false;
    }

    /**
     * Align for the header, read the length and metadata flags and adjust the
     * byte limits to the end of the message.
     */
    @Override
    public void start() {
        rollback = false;
        wire.getValueIn().resetState();
        wire.getValueOut().resetBetweenDocuments();
        readPosition = readLimit = -1;
        @NotNull final Bytes<?> bytes = wire.bytes();
        setStart(bytes.readPosition());

        present = false;
        if (bytes.readRemaining() < 4) {
            notComplete = false;
            return;
        }

        // align
        long position = bytes.readPositionForHeader(wire.usePadding());

        int header = bytes.readVolatileInt(position);
        notComplete = Wires.isNotComplete(header); // || isEndOfFile
        if (header == 0 || (wire.notCompleteIsNotPresent() && notComplete)) {
            return;
        }

        bytes.readSkip(4);

        final int len = lengthOf(header);

        if (len > bytes.readRemaining()) {
            bytes.readSkip(-4);
            return;
        }

        metaData = Wires.isReadyMetaData(header);
        readLimit = bytes.readLimit();
        readPosition = bytes.readPosition() + len;

        bytes.readLimit(readPosition);
        present = true;
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
    public void rollbackOnClose() {
        rollback = true;
    }

    /**
     * Record the starting byte position of the current document and update
     * {@code lastStart}.
     *
     * @param start byte offset within the wire
     */
    public void setStart(long start) {
        this.start = start;
        this.lastStart = start;
    }

    /**
     * Dump the current message as a YAML string via
     * {@link Wires#fromSizePrefixedBlobs(ReadDocumentContext)}.
     */
    @Override
    public String toString() {
        return Wires.fromSizePrefixedBlobs(this);
    }
}
