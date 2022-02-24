/*
 * Copyright 2016-2020 chronicle.software
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
import org.jetbrains.annotations.NotNull;

public class TextWriteDocumentContext implements WriteDocumentContext {
    protected Wire wire;
    private boolean metaData;
    private volatile boolean notComplete;
    private int count = 0;
    private boolean chainedElement;
    private boolean rollback;

    public TextWriteDocumentContext(Wire wire) {
        this.wire = wire;
    }

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
        count--;
        if (count > 0)
            return;
        @NotNull Bytes bytes = wire().bytes();
        if (rollback) {
            bytes.writePosition(bytes.readPosition());
            return;
        }
        long l = bytes.writePosition();
        if (l < 1 || bytes.peekUnsignedByte(l - 1) >= ' ')
            bytes.append('\n');
        if (!(wire() instanceof JSONWire))
            bytes.append("...\n");
        wire().getValueOut().resetBetweenDocuments();
        notComplete = false;
    }

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
