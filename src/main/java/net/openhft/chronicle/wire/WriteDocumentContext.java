/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
        tmpHeader = metaDataBit | Wires.NOT_READY | Wires.UNKNOWN_LENGTH;
        bytes.writeOrderedInt(tmpHeader);
    }

    @Override
    public boolean isMetaData() {
        return metaDataBit != 0;
    }

    @Override
    public void close() {
        Bytes bytes = wire().bytes();
        long position1 = bytes.writePosition();
        if (position1 < position)
            System.out.println("Message truncated from " + position + " to " + position1);
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
}
