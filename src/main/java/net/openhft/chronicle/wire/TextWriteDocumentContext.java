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
import org.jetbrains.annotations.NotNull;

/**
 * Provides a concrete implementation of the {@link WriteDocumentContext} for text-based wire representations.
 * This class manages and tracks the state of the document being written and contains functionalities
 * for starting a new write transaction in the document.
 * <p>
 * While writing, it can be ensured that meta-data is correctly specified, and the position of the write
 * operation is recorded.
 */
public class TextWriteDocumentContext implements WriteDocumentContext {

    // The wire used for writing.
    protected Wire wire;

    // Flag to check if the current data being written is meta-data.
    private boolean metaData;

    // Indicates if the write operation is completed.
    private volatile boolean notComplete;

    // Maintains the count of start operations.
    protected int count = 0;

    // Indicates if the current element is chained to the previous one.
    private boolean chainedElement;

    // Indicates if the current write operation should be rolled back.
    private boolean rollback;

    // Maintains the current position of the write operation.
    protected long position;

    /**
     * Constructs a new context for the specified wire.
     *
     * @param wire The wire instance to be used for writing
     */
    public TextWriteDocumentContext(Wire wire) {
        this.wire = wire;
    }

    /**
     * Starts a new write transaction in the document. If a transaction is already in progress,
     * this method ensures the meta-data flag is consistent. It also sets the write position
     * and other state flags to their initial values.
     *
     * @param metaData Indicates if the data being written is meta-data
     */
    public void start(boolean metaData) {
        count++;
        if (count > 1) {
            assert metaData == isMetaData();
            return;
        }
        this.metaData = metaData;
        if (metaData)
            wire().writeComment("meta-data");
        notComplete = true;
        chainedElement = false;
        rollback = false;
        position = wire().bytes().writePosition();
    }

    @Override
    public boolean isEmpty() {
        return wire().bytes().writePosition() == position;
    }

    @Override
    public boolean isMetaData() {
        return metaData;
    }

    @Override
    @SuppressWarnings("rawtypes")
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
            bytes.writePosition(bytes.readPosition());
            return;
        }
        long l = bytes.writePosition();
        if (!(wire() instanceof JSONWire)) {
            if (l < 1 || bytes.peekUnsignedByte(l - 1) >= ' ')
                bytes.append('\n');
            BytesUtil.combineDoubleNewline(bytes);
            bytes.append("...\n");
        }
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
        metaData = false;
        rollback = false;
        notComplete = false;
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
