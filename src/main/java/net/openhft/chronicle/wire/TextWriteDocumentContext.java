/*
 * Copyright 2016-2025 chronicle.software
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
 * Concrete {@link WriteDocumentContext} for text-based wire formats.
 * Tracks the state of the document being written and appends the
 * {@code ...} separator when the document is closed.
 */
public class TextWriteDocumentContext implements WriteDocumentContext {

    /** The underlying wire used to write text. */
    protected Wire wire;

    /** Set when this document represents meta-data. */
    private boolean metaData;

    /** True until the document is closed. */
    private volatile boolean notComplete;

    /** Nesting level of {@link #start(boolean)} calls. */
    protected int count = 0;

    /** Indicates that this context is part of a chain of documents. */
    private boolean chainedElement;

    /** Marks the current document so it will be rolled back on close. */
    private boolean rollback;

    /** Start position recorded at {@link #start(boolean)}. */
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
     * Prepares for writing a new text document. If {@code isMetaData} is true a
     * {@code "meta-data"} comment is written. Nested calls are allowed but only
     * the outermost call performs full initialisation.
     *
     * @param isMetaData true if the document represents meta-data
     */
    public void start(boolean isMetaData) {
        count++;
        if (count > 1) {
            assert isMetaData == isMetaData();
            return;
        }
        this.metaData = isMetaData;
        if (isMetaData)
            wire().writeComment("meta-data");
        notComplete = true;
        chainedElement = false;
        rollback = false;
        position = wire().bytes().writePosition();
    }

    @Override
    /**
     * Checks whether any bytes have been written since
     * {@link #start(boolean)} was called.
     */
    public boolean isEmpty() {
        return wire().bytes().writePosition() == position;
    }

    @Override
    /**
     * Indicates whether the current document is meta-data rather than user data.
     */
    public boolean isMetaData() {
        return metaData;
    }

    @Override
    /**
     * Completes the document. Unless part of a chain or rolled back, a newline
     * and the {@code ...} end marker are appended and the value output is
     * reset.
     */
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
    /**
     * If the document has not been closed, mark it for rollback and close it,
     * discarding any text that has been written so far.
     */
    public void rollbackIfNotComplete() {
        if (!notComplete) return;
        chainedElement = false;
        count = 1;
        rollback = true;
        close();
    }

    @Override
    /**
     * Marks this context so that closing it rolls back any writes rather than
     * committing them.
     */
    public void rollbackOnClose() {
        rollback = true;
    }

    @Override
    /**
     * Clears this context for reuse, closing it first if required.
     */
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
    /**
     * Returns true if this context forms part of a chained write
     * sequence.
     */
    public boolean chainedElement() {
        return chainedElement;
    }

    @Override
    /**
     * Sets whether this context is chained to the previous document.
     */
    public void chainedElement(boolean isChained) {
        this.chainedElement = isChained;
    }

    @Override
    /**
     * Always returns {@code false} for a write context as there is no
     * existing data to read.
     */
    public boolean isPresent() {
        return false;
    }

    @Override
    /**
     * Returns the wire used to output this document.
     */
    public Wire wire() {
        return wire;
    }

    @Override
    /**
     * Not supported for text documents.
     */
    public long index() {
        throw new UnsupportedOperationException();
    }

    @Override
    /**
     * Always returns {@code -1} for text documents.
     */
    public int sourceId() {
        return -1;
    }

    @Override
    /**
     * Returns {@code true} while the document is open for writing.
     */
    public boolean isNotComplete() {
        return notComplete;
    }
}
