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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.openhft.chronicle.wire.Wires.lengthOf;

public class BinaryReadDocumentContext implements ReadDocumentContext {
    static final StringBuilderPool SBP = new StringBuilderPool();
    private final boolean ensureFullRead;
    public long start = -1;
    public long lastStart = -1;
    @Nullable
    protected AbstractWire wire;
    protected boolean present;
    protected boolean notComplete;
    protected long readPosition;
    protected long readLimit;
    protected boolean metaData;
    protected boolean rollback;

    public BinaryReadDocumentContext(@Nullable Wire wire) {
        this(wire, wire != null && wire.getValueIn() instanceof BinaryWire.DeltaValueIn);
    }

    public BinaryReadDocumentContext(@Nullable Wire wire, boolean ensureFullRead) {
        this.wire = (AbstractWire) wire;
        this.ensureFullRead = ensureFullRead;
    }

    private static void fullReadForDeltaWire(AbstractWire wire0, long start) {
        long readPosition1 = wire0.bytes().readPosition();
        try {
            // we have to read back from the start, as close may have been called in
            // the middle of reading a value
            wire0.bytes().readPosition(start);
            wire0.bytes().readSkip(4);
            while (wire0.hasMore()) {
                final long remaining = wire0.bytes().readRemaining();
                final ValueIn read = wire0.read();
                if (read.isTyped()) {
                    read.skipValue();
                } else {
                    read.text(SBP.acquireStringBuilder());  // todo remove this and use skipValue
                }

                if (wire0.bytes().readRemaining() == remaining) {
                    // stopped making progress, exit loop
                    break;
                }
            }
        } catch (Exception e) {
            // TODO: don't believe this is need any more. Have changed from debug to warn
            Jvm.warn().on(BinaryReadDocumentContext.class, e);
        } finally {
            wire0.bytes().readPosition(readPosition1);
        }
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

    protected boolean rollback() {
        return rollback;
    }

    @Override
    public void close() {
        if (rollbackIfNeeded())
            return;

        long readLimit0 = this.readLimit;
        long readPosition0 = this.readPosition;

        AbstractWire wire0 = this.wire;
        if (present && ensureFullRead && start >= 0 && wire0 != null && wire0.hasMore()) {
            fullReadForDeltaWire(wire0, start);
        }

        start = -1;
        if (readLimit0 > 0 && wire0 != null) {
            @NotNull final Bytes<?> bytes = wire0.bytes();
            if (bytes.readPosition() < readPosition0)
                Jvm.warn().on(getClass(), "The readPosition was invalid " + bytes.readPosition() + " < " + readPosition0);
            if (bytes.readLimit() > readLimit0)
                Jvm.warn().on(getClass(), "The readLimit was invalid " + bytes.readLimit() + " < " + readLimit0);
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
     * Rolls back document context state to a one before opening if rollback marker is set.
     *
     * @return If rolled back.
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

    public void setStart(long start) {
        this.start = start;
        this.lastStart = start;
    }

    @Override
    public String toString() {
        return Wires.fromSizePrefixedBlobs(this);
    }
}
