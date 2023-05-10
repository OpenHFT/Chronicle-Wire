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
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;

import static net.openhft.chronicle.wire.Wires.toIntU30;

public class BinaryWriteDocumentContext implements WriteDocumentContext {
    protected Wire wire;
    protected long position = -1;
    protected int tmpHeader;
    protected int count = 0;
    private int metaDataBit;
    private volatile boolean notComplete;
    private boolean chainedElement;

    public BinaryWriteDocumentContext(Wire wire) {
        this.wire = wire;
    }

    public void start(boolean metaData) {
        count++;
        if (count > 1) {
            assert metaData == isMetaData();
            return;
        }
        @NotNull Bytes<?> bytes = wire().bytes();
        bytes.writePositionForHeader(wire.usePadding());
        bytes.writeHexDumpDescription("msg-length");
        this.position = bytes.writePosition();
        metaDataBit = metaData ? Wires.META_DATA : 0;
        tmpHeader = metaDataBit | Wires.NOT_COMPLETE | Wires.UNKNOWN_LENGTH;
        bytes.writeInt(tmpHeader);
        notComplete = true;
        chainedElement = false;
    }

    @Override
    public boolean isEmpty() {
        return notComplete && wire().bytes().writePosition() == position + 4;
    }

    @Override
    public void reset() {
        if (count > 0)
            close();
        count = 0;
        position = 0;
        metaDataBit = 0;
        tmpHeader = 0;
        notComplete = false;
        chainedElement = false;
    }

    @Override
    public boolean isMetaData() {
        return metaDataBit != 0;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void close() {
        if (chainedElement)
            return;
        if (count == 0)
            throw new IllegalStateException("count == 0");
        count--;
        if (count > 0)
            return;
        if (checkResetOpened())
            return;
        @NotNull Bytes<?> bytes = wire().bytes();
        long position1 = bytes.writePosition();
//        if (position1 < position)
//            System.out.println("Message truncated from " + position + " to " + position1);
        long length0 = position1 - position - 4;
        if (length0 > Integer.MAX_VALUE && bytes instanceof HexDumpBytes)
            length0 = (int) length0;
        int length = metaDataBit | toIntU30(length0, "Document length %,d out of 30-bit int range.");
        if (wire.usePadding())
            bytes.testAndSetInt(position, tmpHeader, length);
        else
            bytes.writeInt(position, length);
        wire().getValueOut().resetBetweenDocuments();
        notComplete = false;
    }

    protected boolean checkResetOpened() {
        if (!notComplete)
            Jvm.warn().on(getClass(), "Closing but not opened");
        boolean wasOpened = notComplete;
        notComplete = false;
        return !wasOpened;
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

    protected long position() {
        return position;
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
