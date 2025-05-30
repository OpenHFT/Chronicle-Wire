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
 * {@link WriteDocumentContext} for text based wires such as YAML.  It writes
 * document separators and tracks whether the document has been completed.
 */
public class TextWriteDocumentContext implements WriteDocumentContext {

    /** Wire used for output. */
    protected Wire wire;

    /** Whether the document is metadata. */
    private boolean metaData;

    /** True while the document is still open. */
    private volatile boolean notComplete;

    /** Nesting count for {@link #start(boolean)} calls. */
    protected int count = 0;

    /** Whether this document is part of a chained write sequence. */
    private boolean chainedElement;

    /** If true the document will be rolled back on close. */
    private boolean rollback;

    /** Position where the document started. */
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
     * Begin writing a new text document.  Nested calls are allowed but only the
     * outermost call performs the initialisation.  The starting write position
     * is recorded so that the document can be rolled back if required.
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

    /**
     * Returns {@code true} if no bytes have been written since the document was
     * {@link #start(boolean) started}.
     */
    @Override
    public boolean isEmpty() {
        return wire().bytes().writePosition() == position;
    }

    @Override
    public boolean isMetaData() {
        return metaData;
    }

    /**
     * Finalise the document.  If the context is part of a chained call the
     * close is ignored until the final call closes it.
     */
    @Override
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

    /**
     * Roll back the document if it has not yet been completed.
     */
    @Override
    public void rollbackIfNotComplete() {
        if (!notComplete) return;
        chainedElement = false;
        count = 1;
        rollback = true;
        close();
    }

    /**
     * Mark the document for rollback when {@link #close()} is called.
     */
    @Override
    public void rollbackOnClose() {
        rollback = true;
    }

    /**
     * Reset internal state so this context can be reused.
     */
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
