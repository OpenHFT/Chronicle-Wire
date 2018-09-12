/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.openhft.chronicle.wire.Wires.lengthOf;

/*
 * Created by Peter Lawrey on 24/12/15.
 */
public class BinaryReadDocumentContext implements ReadDocumentContext {
    private final boolean ensureFullRead;
    public long start = -1;
    public long lastStart = -1;
    @Nullable
    protected AbstractWire wire;
    protected boolean present, notComplete;
    protected long readPosition, readLimit;
    private boolean metaData;
    private boolean rollback;

    public BinaryReadDocumentContext(@Nullable Wire wire) {
        this(wire, wire != null && wire.getValueIn() instanceof BinaryWire.DeltaValueIn);
    }

    public BinaryReadDocumentContext(@Nullable Wire wire, boolean ensureFullRead) {
        this.wire = (AbstractWire) wire;
        this.ensureFullRead = ensureFullRead;
    }

    @Override
    public boolean isMetaData() {
        return metaData;
    }

    @Override
    public void metaData(boolean metaData) {
        // NOTE: this will not change the entry in the queue just read.
        this.metaData = metaData;
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
        long readLimit = this.readLimit;
        long readPosition = this.readPosition;

        AbstractWire wire0 = this.wire;
        if (ensureFullRead && wire0 != null && wire0.hasMore()) {
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
                        read.text(Wires.acquireStringBuilder());  // todo remove this and use skipValue
                    }

                    if (wire0.bytes().readRemaining() == remaining) {
                        // stopped making progress, exit loop
                        break;
                    }
                }
            } catch (Exception e) {
                Jvm.debug().on(getClass(), e);
            } finally {
                wire0.bytes().readPosition(readPosition1);
            }
        }

        start = -1;
        if (readLimit > 0 && wire0 != null) {
            @NotNull final Bytes<?> bytes = wire0.bytes();
            bytes.readLimit(readLimit);
            bytes.readPosition(readPosition);
        }

        present = false;
    }

    @Override
    public void start() {
        rollback = false;
        wire.getValueOut().resetBetweenDocuments();
        readPosition = readLimit = -1;
        @NotNull final Bytes<?> bytes = wire.bytes();
        setStart(bytes.readPosition());

        present = false;
        if (bytes.readRemaining() < 4) {
            notComplete = false;
            return;
        }

        long position = bytes.readPosition();

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
        return 0;
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
