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

import static net.openhft.chronicle.wire.Wires.isNotReady;
import static net.openhft.chronicle.wire.Wires.lengthOf;

/**
 * Created by peter on 24/12/15.
 */
public class ReadDocumentContext implements DocumentContext {
    protected Wire wire;
    private boolean metaData;
    private boolean present;
    private long readPosition, readLimit;

    public ReadDocumentContext(Wire wire) {
        this.wire = wire;
    }

    @Override
    public boolean isMetaData() {
        return metaData;
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
    public Wire wire() {
        return wire;
    }

    @Override
    public void close() {
        if (readLimit > 0) {
            final Bytes<?> bytes = wire.bytes();
            bytes.readLimit(readLimit);
            bytes.readPosition(readPosition);
        }
    }

    public void start() {
        final Bytes<?> bytes = wire.bytes();
        if (bytes.readRemaining() < 4) {
            present = false;
            readPosition = readLimit = -1;
            return;
        }
        long position = bytes.readPosition();

        int header = bytes.readVolatileInt(position);
        if (header == 0 || isNotReady(header)) {
            present = false;
            return;
        }

        bytes.readSkip(4);

        final int len = lengthOf(header);
        assert len > 0 : "len=" + len;
        metaData = Wires.isReadyMetaData(header);
        if (len > bytes.readRemaining())
            throw new BufferUnderflowException();
        readLimit = bytes.readLimit();
        readPosition = bytes.readPosition() + len;

        bytes.readLimit(readPosition);
        present = true;
    }
}
