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
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.scoped.ScopedResource;
import net.openhft.chronicle.core.scoped.ScopedResourcePool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.openhft.chronicle.wire.Wires.lengthOf;

/**
 * {@link ReadDocumentContext} implementation for length prefixed binary
 * messages.
 */
public class BinaryReadDocumentContext implements ReadDocumentContext {

    /** Start position of the current document. */
    public long start = -1;
    /** Last document start position, used for diagnostics. */
    public long lastStart = -1;
    @Nullable
    protected Wire wire;
    protected boolean present;
    protected boolean notComplete;
    protected long readPosition;
    protected long readLimit;
    protected boolean metaData;
    protected boolean rollback;

    /**
     * Create a new context for the supplied wire.
     *
     * @param wire wire to read from, may be {@code null}
     */
    public BinaryReadDocumentContext(@Nullable Wire wire) {
        this.wire = wire;
    }

    /**
     * @deprecated delta wire support removed
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

    /**
     * Restore the read position/limit unless a rollback was requested.
     */
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
     * Roll back to the {@link #start} position if {@link #rollbackOnClose()} was
     * called.
     *
     * @return {@code true} if a rollback occurred
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
     * Read the length prefix and prepare the next document for reading.
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
     * Record the starting position of the current document.
     */
    public void setStart(long start) {
        this.start = start;
        this.lastStart = start;
    }

    /**
     * Dump the current document as text via
     * {@link Wires#fromSizePrefixedBlobs(DocumentContext)}.
     */
    @Override
    public String toString() {
        return Wires.fromSizePrefixedBlobs(this);
    }
}
