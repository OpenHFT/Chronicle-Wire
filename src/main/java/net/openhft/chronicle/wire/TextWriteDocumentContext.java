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
 * TextWriteDocumentContext is a concrete implementation of the WriteDocumentContext interface,
 * providing methods for writing text data to a document.
 */
public class TextWriteDocumentContext implements WriteDocumentContext {
    protected Wire wire;
    private boolean metaData;
    private volatile boolean notComplete;
    protected int count = 0;
    private boolean chainedElement;
    private boolean rollback;
    protected long position;

    /**
     * Constructs a TextWriteDocumentContext with the provided Wire object.
     *
     * @param wire The Wire object to be used for writing operations.
     */
    public TextWriteDocumentContext(Wire wire) {
        this.wire = wire;
    }

    /**
     * Starts the process of writing data to the document.
     *
     * @param metaData Specifies whether the data being written is metadata.
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

    /**
     * Closes the current document context. If chainedElement is true, no operation is performed.
     * If the rollback flag is set, it will rollback to the previous state.
     */
    @Override
    public boolean isMetaData() {
        return metaData;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void close() {
        if (chainedElement)
            return;
        count--;
        if (count > 0)
            return;
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
        notComplete = false;
    }

    /**
     * Resets the current context, clearing any writing operations or flags that have been set.
     */
    @Override
    public void reset() {
        chainedElement = false;
        if (count > 0)
            close();
        count = 0;
        rollback = false;
        notComplete = false;
    }

    /**
     * Sets the rollback flag to true. It means that the document context will be rolled back to
     * the state before opening when it is closed.
     */
    @Override
    public void rollbackOnClose() {
        rollback = true;
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
