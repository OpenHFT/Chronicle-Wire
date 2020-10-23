/*
 * Copyright 2016-2020 Chronicle Software
 *
 * https://chronicle.software
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
import net.openhft.chronicle.bytes.BytesStore;
import org.jetbrains.annotations.Nullable;

public class TextReadDocumentContext implements ReadDocumentContext {
    public static final BytesStore SOD_SEP = BytesStore.from("---");
    public static final BytesStore EOD_SEP = BytesStore.from("...");
    @Nullable
    protected AbstractWire wire;
    protected boolean present, notComplete;

    private boolean metaData;
    private long readPosition, readLimit;
    private long start = -1;
    private boolean rollback;

    public TextReadDocumentContext(@Nullable AbstractWire wire) {
        this.wire = wire;
    }

    public static void consumeToEndOfMessage(Bytes<?> bytes) {
        while (bytes.readRemaining() > 0) {
            while (bytes.readRemaining() > 0 && bytes.readUnsignedByte() >= ' ') {
                // read skips forward.
            }
            if (isEndOfMessage(bytes)) {
                break;
            }
        }
    }

    public static boolean isEndOfMessage(Bytes<?> bytes) {
        return (bytes.startsWith(SOD_SEP) || bytes.startsWith(EOD_SEP))
                && isWhiteSpaceAt(bytes);
    }

    protected static boolean isWhiteSpaceAt(Bytes<?> bytes) {
        return bytes.peekUnsignedByte(bytes.readPosition() + 3) <= ' ';
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

    @Override
    public void close() {
        long readLimit = this.readLimit;
        long readPosition = this.readPosition;

        AbstractWire wire0 = this.wire;
        wire0.bytes.readLimit(readLimit);

        if (rollback) {
            if (start > -1)
                wire0.bytes.readPosition(start);

            rollback = false;
        } else {
            wire0.bytes.readPosition(readPosition);
        }
        start = -1;

        wire.getValueIn().resetState();
        present = false;
    }

    @Override
    public void start() {
        wire.getValueIn().resetState();
        Bytes<?> bytes = wire.bytes();

        present = false;
        wire.consumePadding();
        if (bytes.startsWith(EOD_SEP) && isWhiteSpaceAt(bytes))
            skipSep(bytes);
        if (bytes.startsWith(SOD_SEP) && isWhiteSpaceAt(bytes))
            skipSep(bytes);

        if (bytes.readRemaining() < 1) {
            readLimit = readPosition = bytes.readLimit();
            notComplete = false;
            return;
        }

        start = bytes.readPosition();
        consumeToEndOfMessage(bytes);

        metaData = false;
        readLimit = bytes.readLimit();
        readPosition = bytes.readPosition();

        bytes.readLimit(bytes.readPosition());
        bytes.readPosition(start);
        present = true;
    }

    protected void skipSep(Bytes<?> bytes) {
        bytes.readSkip(3);
        wire.getValueIn().resetState();
        wire.consumePadding();
    }

    @Override
    public void rollbackOnClose() {
        rollback = true;
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
    public String toString() {
        return wire.toString();
    }
}
