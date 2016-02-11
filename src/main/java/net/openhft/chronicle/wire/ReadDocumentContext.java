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

import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.wire.Wires.*;

/**
 * Created by peter on 24/12/15.
 */
public class ReadDocumentContext implements DocumentContext {
    private final InternalWire wire;
    private boolean data;
    private boolean present;
    private long readPosition, readLimit;
    private long index;

    public ReadDocumentContext(Wire wire) {
        this.wire = (InternalWire) wire;
    }

    @Override
    public boolean isMetaData() {
        return !data && present;
    }

    @Override
    public boolean isPresent() {
        return present;
    }

    public void closeReadPosition(long readPosition) {
        this.readPosition = readPosition;
    }

    public void closeReadLimit(long readLimit) {
        this.readLimit = readLimit;
    }


    @Override
    public boolean isData() {
        return data && present;
    }

    @Override
    public Wire wire() {
        return wire;
    }

    @Override
    public void close() {
        if (readLimit > 0) {
            final Bytes<?> bytes = wire.bytes();
            bytes.readPosition(readPosition);
            bytes.readLimit(readLimit);
        }
    }

    @Override
    public long index() {
        return index;
    }

    public void start() {
        start(true, wire.bytes().readPosition());
    }

    public void start(boolean next, long index) {
        this.index = index;
        final Bytes<?> bytes = wire.bytes();
        if (!next || bytes.readRemaining() < 4) {
            present = false;
            readPosition = readLimit = -1;
            return;
        }
        long position = bytes.readPosition();

        int header = bytes.readVolatileInt(position);
        if (!isKnownLength(header) || !isReady(header)) {
            present = false;
            return;
        }

        bytes.readSkip(4);
        final boolean ready = isReady(header);
        final int len = lengthOf(header);
        assert len > 0 : "len=" + len;
        data = Wires.isData(header);
        wire.setReady(ready);
        if (len > bytes.readRemaining())
            throw new BufferUnderflowException();
        readLimit = bytes.readLimit();
        readPosition = bytes.readPosition() + len;

        bytes.readLimit(readPosition);
        present = true;
    }


}
