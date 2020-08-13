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
import org.jetbrains.annotations.NotNull;

public class TextWriteDocumentContext implements WriteDocumentContext {
    protected Wire wire;
    private boolean metaData;
    private boolean notComplete;

    public TextWriteDocumentContext(Wire wire) {
        this.wire = wire;
    }

    public void start(boolean metaData) {
        this.metaData = metaData;
        if (metaData)
            wire().writeComment("meta-data");
        notComplete = true;
    }

    @Override
    public boolean isMetaData() {
        return metaData;
    }

    @Override
    public void metaData(boolean metaData) {
        if (metaData != this.metaData)
            throw new UnsupportedOperationException("cannot change metaData status");
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void close() {
        @NotNull Bytes bytes = wire().bytes();
        long l = bytes.writePosition();
        if (l < 1 || bytes.peekUnsignedByte(l - 1) >= ' ')
            bytes.append('\n');
        if (TextMethodWriterInvocationHandler.ENABLE_EOD)
            bytes.append("---\n");
        wire().getValueOut().resetBetweenDocuments();
        notComplete = false;
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
