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
import net.openhft.chronicle.bytes.BytesStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/*
 * Created by Peter Lawrey on 24/12/15.
 */
public class TextReadDocumentContext implements ReadDocumentContext {
    public static final BytesStore MSG_SEP = BytesStore.from("---");
    @Nullable
    protected TextWire wire;
    protected boolean present, notComplete;

    private boolean metaData;
    private long readPosition, readLimit;

    public TextReadDocumentContext(@Nullable TextWire wire) {
        this.wire = wire;
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

    @Override
    public void close() {
        long readLimit = this.readLimit;
        long readPosition = this.readPosition;

        AbstractWire wire0 = this.wire;
        wire0.bytes.readLimit(readLimit);
        wire0.bytes.readPosition(readPosition);

        present = false;
    }

    @Override
    public void start() {
        wire.getValueOut().resetBetweenDocuments();
        @NotNull final Bytes<?> bytes = wire.bytes();

        present = false;
        wire.consumePadding();
        if (wire.bytes().startsWith(MSG_SEP)) {
            wire.bytes().readSkip(3);
            wire.consumePadding();
        }
        if (bytes.readRemaining() < 1) {
            readLimit = readPosition = bytes.readLimit();
            notComplete = false;
            return;
        }

        long position = bytes.readPosition();
        wire.getValueIn().skipValue();
        metaData = false;
        readLimit = bytes.readLimit();
        readPosition = bytes.readPosition();

        bytes.readLimit(bytes.readPosition());
        bytes.readPosition(position);
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
    public String toString() {
        return Wires.fromSizePrefixedBlobs(this);
    }
}
