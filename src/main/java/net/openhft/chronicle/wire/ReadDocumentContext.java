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

import static net.openhft.chronicle.wire.Wires.isNotReady;
import static net.openhft.chronicle.wire.Wires.lengthOf;

/**
 * Created by peter on 24/12/15.
 */
public class ReadDocumentContext implements DocumentContext {
    protected AbstractWire wire;
    protected boolean present;
    private boolean metaData;
    private long readPosition, readLimit;

    public ReadDocumentContext(Wire wire) {
        this.wire = (AbstractWire) wire;
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
        if (readLimit > 0) {
            final Bytes<?> bytes = wire.bytes();
            bytes.readLimit(readLimit);
            bytes.readPosition(readPosition);
        }
    }

    public void start() {
        readPosition = readLimit = -1;
        final Bytes<?> bytes = wire.bytes();

        if (bytes.readRemaining() < 4) {
            present = false;
            // I think this has no effect 1 as its only set on close
            readPosition = readLimit = -1;

            return;
        }
        long position = bytes.readPosition();

        int header = bytes.readVolatileInt(position);
        if (header == 0 || isNotReady(header)) {
            present = false;
            return;
        }

        bytes.readSkip(4);

        final int len = lengthOf(header);

        if (len > bytes.readRemaining()) {
            bytes.readSkip(-4);
            present = false;
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
}
