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

/**
 * Created by peter on 24/12/15.
 */
public class ReadDocumentContext implements DocumentContext {
    protected AbstractWire wire;
    private final boolean ensureFullRead;
    protected boolean present, notComplete;
    long start = -1;
    private boolean metaData;
    private long readPosition, readLimit;

    public ReadDocumentContext(@Nullable Wire wire) {
        this(wire, wire != null && wire.getValueIn() instanceof BinaryWire.DeltaValueIn);
    }

    public ReadDocumentContext(@Nullable Wire wire, boolean ensureFullRead) {
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

    public void closeReadPosition(long readPosition) {
        this.readPosition = readPosition;
    }

    public void closeReadLimit(long readLimit) {
        this.readLimit = readLimit;
    }

    @Override
    public Wire wire() {
        return wire;
    }


    @Override
    public void close() {
        long readLimit = this.readLimit;
        long readPosition = this.readPosition;

        AbstractWire wire0 = this.wire;
        if (ensureFullRead && wire0 != null && !wire0.isEmpty()) {
            try {
                // we have to read back from the start, as close may have been called in
                // the middle of reading a value
                wire0.bytes().readPosition(start);
                wire0.bytes().writeSkip(4);
                while (wire0.hasMore()) {

                    final StringBuilder value = Wires.acquireStringBuilder();
                    final ValueIn read = wire0.read();

                    if (read.isTyped()) {
                        read.skipValue();
                    } else
                        read.text(value);  // todo remove this and use skipValue
                }

            } catch (Exception e) {
                Jvm.debug().on(getClass(), e);
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

    public void start() {
        wire.getValueOut().resetBetweenDocuments();
        readPosition = readLimit = -1;
        @NotNull final Bytes<?> bytes = wire.bytes();
        start = bytes.readPosition();

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
}
