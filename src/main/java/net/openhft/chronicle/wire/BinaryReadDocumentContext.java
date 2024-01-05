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
 * This is the BinaryReadDocumentContext class which implements the ReadDocumentContext interface.
 * It provides an implementation tailored for reading from binary document contexts and ensures
 * full read capability if required.
 */
public class BinaryReadDocumentContext implements ReadDocumentContext {

    private final boolean ensureFullRead; // Determines if the context ensures full reading.
    public long start = -1;
    public long lastStart = -1;
    @Nullable
    protected AbstractWire wire;  // The underlying wire representation of the document.
    protected boolean present;    // Indicates if the context is currently present.
    protected boolean notComplete;
    protected long readPosition;  // The current read position within the document.
    protected long readLimit;     // The boundary up to which reading should occur in the document.
    protected boolean metaData;   // Flag to indicate if the current reading involves metadata.
    protected boolean rollback;   // Determines if the context should roll back.

    /**
     * Constructor that initializes the BinaryReadDocumentContext using the provided wire.
     * It also determines if a full read should be ensured based on the wire type.
     *
     * @param wire The wire used for reading the document.
     */
    public BinaryReadDocumentContext(@Nullable Wire wire) {
        this(wire, wire != null && wire.getValueIn() instanceof BinaryWire.DeltaValueIn);
    }

    /**
     * Constructor that initializes the BinaryReadDocumentContext using the provided wire and
     * a flag to determine if a full read should be ensured.
     *
     * @param wire           The wire used for reading the document.
     * @param ensureFullRead Flag to determine if full reading is required.
     */
    public BinaryReadDocumentContext(@Nullable Wire wire, boolean ensureFullRead) {
        this.wire = (AbstractWire) wire;
        this.ensureFullRead = ensureFullRead;
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
     * Performs a full read for a delta wire starting from a specified position.
     * This is used to ensure that all content of the wire is read.
     *
     * @param wire0 The delta wire to read from.
     * @param start The starting position for the full read.
     */
    private static void fullReadForDeltaWire(AbstractWire wire0, long start) {
        long readPosition1 = wire0.bytes().readPosition();
        try {
            // We reset the position to start as close might have been called during reading.
            wire0.bytes().readPosition(start);
            wire0.bytes().readSkip(4);
            while (wire0.hasMore()) {
                final long remaining = wire0.bytes().readRemaining();
                final ValueIn read = wire0.read();
                if (read.isTyped()) {
                    read.skipValue();
                } else {
                    try (final ScopedResource<StringBuilder> tlSb = SBP.get()){
                        read.text(tlSb.get());  // todo remove this and use skipValue
                    }
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
     * Rolls back the document context to its state before opening, if the rollback marker is set.
     * Resets relevant attributes and updates the read position and limit accordingly.
     *
     * @return {@code true} if the context was rolled back, {@code false} otherwise.
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

    /**
     * Sets the start position of the document context and updates the last start position.
     * This is useful to keep track of the beginning position for reading purposes.
     *
     * @param start The new starting position to set.
     */
    public void setStart(long start) {
        this.start = start;
        this.lastStart = start;
    }

    @Override
    public String toString() {
        return Wires.fromSizePrefixedBlobs(this);
    }
}
