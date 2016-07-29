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

import static net.openhft.chronicle.wire.Wires.toIntU30;

/**
 * Created by peter on 24/12/15.
 */
public class WriteDocumentContext implements DocumentContext {
    protected Wire wire;
    protected long position = -1;
    protected int tmpHeader;
    private int metaDataBit;

    public WriteDocumentContext(Wire wire) {
        this.wire = wire;
    }

    public void start(boolean metaData) {
        Bytes<?> bytes = wire().bytes();
        this.position = bytes.writePosition();
        metaDataBit = metaData ? Wires.META_DATA : 0;
        tmpHeader = metaDataBit | Wires.NOT_COMPLETE | Wires.UNKNOWN_LENGTH;
        bytes.writeOrderedInt(tmpHeader);
    }

    @Override
    public boolean isMetaData() {
        return metaDataBit != 0;
    }

    @Override
    public void metaData(boolean metaData) {
        metaDataBit = metaData ? Wires.META_DATA : 0;
    }

    @Override
    public void close() {
        Bytes bytes = wire().bytes();
        long position1 = bytes.writePosition();
//        if (position1 < position)
//            System.out.println("Message truncated from " + position + " to " + position1);
        int length = metaDataBit | toIntU30(position1 - position - 4, "Document length %,d out of 30-bit int range.");
        if (!bytes.compareAndSwapInt(position, tmpHeader, length))
            throw new IllegalStateException("Header at " + position + " overwritten with " + Integer.toHexString(bytes.readInt(position)));

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
        return true;
    }
}
