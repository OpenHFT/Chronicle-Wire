/*
 *
 *  *     Copyright (C) ${YEAR}  higherfrequencytrading.com
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU Lesser General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU Lesser General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU Lesser General Public License
 *  *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.util.UTF8StringInterner;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.threads.BusyPauser;
import net.openhft.chronicle.threads.Pauser;
import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.StreamCorruptedException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by peter on 10/03/16.
 */
public abstract class AbstractWire implements Wire {
    protected static final UTF8StringInterner UTF8_INTERNER = new UTF8StringInterner(512);
    protected static final boolean ASSERTIONS;

    static {
        boolean assertions = false;
        assert assertions = true;
        ASSERTIONS = assertions;
    }

    protected final Bytes<?> bytes;
    protected final boolean use8bit;
    protected Pauser pauser = BusyPauser.INSTANCE;
    protected ClassLookup classLookup = ClassAliasPool.CLASS_ALIASES;

    public AbstractWire(Bytes bytes, boolean use8bit) {
        this.bytes = bytes;
        this.use8bit = use8bit;
    }

    private static long throwNotEnoughSpace(int maxlen, Bytes<?> bytes) {
        throw new IllegalStateException("not enough space to write " + maxlen + " was " + bytes.writeRemaining());
    }

    private static void throwHeaderOverwritten(long position, int expectedHeader, Bytes<?> bytes) throws StreamCorruptedException {
        throw new StreamCorruptedException("Data at " + position + " overwritten? Expected: " + Integer.toHexString(expectedHeader) + " was " + Integer.toHexString(bytes.readVolatileInt(position)));
    }

    private static void throwLengthMismatch(int length, int actualLength) throws StreamCorruptedException {
        throw new StreamCorruptedException("Wrote " + actualLength + " when " + length + " was set initially.");
    }

    @Override
    public Pauser pauser() {
        return pauser;
    }

    @Override
    public void pauser(Pauser pauser) {
        this.pauser = pauser;
    }

    @Override
    public void clear() {
        bytes.clear();
    }

    @Override
    public void classLookup(ClassLookup classLookup) {
        this.classLookup = classLookup;
    }

    @Override
    public ClassLookup classLookup() {
        return classLookup;
    }

    @NotNull
    @Override
    public Bytes<?> bytes() {
        return bytes;
    }

    @Override
    public boolean hasMore() {
        consumePadding();
        return bytes.readRemaining() > 0;
    }

    @Override
    public boolean readDataHeader() throws EOFException {
        bytes.readLimit(bytes.capacity());
        for (; ; ) {
            int header = bytes.readVolatileInt(bytes.readPosition());
            if (Wires.isReady(header)) {
                if (header == Wires.NOT_INITIALIZED)
                    return false;
                if (Wires.isReadyData(header)) {
                    return true;
                }
                bytes.readSkip(Wires.lengthOf(header) + Wires.SPB_HEADER_SIZE);
            } else {
                if (header == Wires.END_OF_DATA)
                    throw new EOFException();
                return false;
            }
        }
    }

    @Override
    public void readAndSetLength(long position) {
        int header = bytes.readVolatileInt(bytes.readPosition());
        if (Wires.isReady(header)) {
            if (header == Wires.NOT_INITIALIZED)
                throw new IllegalStateException();
            long start = position + Wires.SPB_HEADER_SIZE;
            bytes.readPositionRemaining(start, Wires.lengthOf(header));
            return;
        }
        throw new IllegalStateException();
    }

    @Override
    public void readMetaDataHeader() {
        int header = bytes.readVolatileInt(bytes.readPosition());
        if (Wires.isReady(header)) {
            if (header == Wires.NOT_INITIALIZED)
                throw new IllegalStateException("Meta data not initialised");
            if (Wires.isReadyMetaData(header)) {
                setLimitPosition(header);
                return;
            }
        }
        throw new IllegalStateException("Meta data not ready " + Integer.toHexString(header));
    }

    private void setLimitPosition(int header) {
        bytes.readLimit(bytes.readPosition() + Wires.lengthOf(header) + Wires.SPB_HEADER_SIZE)
                .readSkip(Wires.SPB_HEADER_SIZE);
    }

    @Override
    public void readFirstHeader(long timeout, TimeUnit timeUnit) throws TimeoutException, StreamCorruptedException {
        int header;
        for (; ; ) {
            header = bytes.readVolatileInt(0L);
            if (Wires.isReady(header)) {
                break;
            }
            pauser.pause(timeout, timeUnit);
        }
        pauser.reset();
        int len = Wires.lengthOf(header);
        if (!Wires.isReadyMetaData(header) || len > 64 << 10)
            throw new StreamCorruptedException("Unexpected magic number " + Integer.toHexString(header));
        bytes.readPositionRemaining(Wires.SPB_HEADER_SIZE, len);
    }

    @Override
    public long writeHeader(int length, long timeout, TimeUnit timeUnit) throws TimeoutException, EOFException {
        if (length < 0 || length > Wires.MAX_LENGTH)
            throw new IllegalArgumentException();
        long pos = bytes.writePosition();

        if (bytes.compareAndSwapInt(pos, 0, Wires.NOT_READY | length)) {
            int maxlen = length == Wires.UNKNOWN_LENGTH ? Wires.MAX_LENGTH : length;
            if (maxlen > bytes.writeRemaining())
                return throwNotEnoughSpace(maxlen, bytes);
            bytes.writePositionRemaining(pos + Wires.SPB_HEADER_SIZE, maxlen);
            return pos;
        }
        return writeHeader0(length, timeout, timeUnit);
    }

    private long writeHeader0(int length, long timeout, TimeUnit timeUnit) throws TimeoutException, EOFException {
        if (length < 0 || length > Wires.MAX_LENGTH)
            throw new IllegalArgumentException();
        long pos = bytes.writePosition();
//        long start = System.nanoTime();
        try {
//            for (int i = 0; ; i++) {
            for (; ; ) {
                if (bytes.compareAndSwapInt(pos, 0, Wires.NOT_READY | length)) {
                    bytes.writePosition(pos + Wires.SPB_HEADER_SIZE);
                    int maxlen = length == Wires.UNKNOWN_LENGTH ? Wires.MAX_LENGTH : length;
                    if (maxlen > bytes.writeRemaining())
                        throwNotEnoughSpace(maxlen, bytes);
                    bytes.writeLimit(bytes.writePosition() + maxlen);
//                    long time =System.nanoTime() - start;
//                    if (time > 20e3)
//                        System.out.println(time/1000+" "+i);
                    return pos;
                }
                pauser.pause(timeout, timeUnit);
                int header = bytes.readVolatileInt(pos);
                // two states where it is unable to continue.
                if (header == Wires.END_OF_DATA)
                    throw new EOFException();
                if (header == Wires.NOT_READY_UNKNOWN_LENGTH)
                    continue;
                int len = Wires.lengthOf(header);
                pos += len + Wires.SPB_HEADER_SIZE; // length of message plus length of header
            }
        } finally {
            pauser.reset();
        }
    }

    @Override
    public void updateHeader(int length, long position, boolean metaData) throws StreamCorruptedException {
        long pos = bytes.writePosition();
        int actualLength = Maths.toUInt31(pos - position - 4);
        int expectedHeader = Wires.NOT_READY | length;
        if (length == Wires.UNKNOWN_LENGTH)
            length = actualLength;
        else if (length < actualLength)
            throwLengthMismatch(length, actualLength);
        int header = length;
        if (metaData) header |= Wires.META_DATA;
        if (ASSERTIONS) {
            if (!bytes.compareAndSwapInt(position, expectedHeader, header))
                throwHeaderOverwritten(position, expectedHeader, bytes);
        } else {
            bytes.writeOrderedInt(position, header);
        }
        bytes.writeLimit(bytes.capacity());
    }

    @Override
    public boolean writeFirstHeader() {
        boolean cas = bytes.compareAndSwapInt(0L, 0, Wires.NOT_READY_UNKNOWN_LENGTH);
        if (cas)
            bytes.writeSkip(Wires.SPB_HEADER_SIZE);
        return cas;
    }

    @Override
    public void updateFirstHeader() {
        long pos = bytes.writePosition();
        long actualLength = pos - Wires.SPB_HEADER_SIZE;
        if (actualLength >= 1 << 30)
            throw new IllegalStateException("Header too large was " + actualLength);
        int header = (int) (Wires.META_DATA | actualLength);
        if (!bytes.compareAndSwapInt(0L, Wires.NOT_READY_UNKNOWN_LENGTH, header))
            throw new IllegalStateException("Data at 0 overwritten? Expected: " + Integer.toHexString(Wires.NOT_READY_UNKNOWN_LENGTH) + " was " + Integer.toHexString(bytes.readVolatileInt(0L)));
    }

    @Override
    public void writeEndOfWire(long timeout, TimeUnit timeUnit) throws TimeoutException {
        long pos = bytes.writePosition();
        try {
            for (; ; ) {
                if (bytes.compareAndSwapInt(pos, 0, Wires.END_OF_DATA)) {
                    bytes.writePosition(pos + Wires.SPB_HEADER_SIZE);
                    return;
                }
                pauser.pause(timeout, timeUnit);
                int header = bytes.readVolatileInt(pos);
                // two states where it is unable to continue.
                if (header == Wires.END_OF_DATA)
                    return; // already written.
                if (header == Wires.NOT_READY_UNKNOWN_LENGTH)
                    continue;
                int len = Wires.lengthOf(header);
                pos += len + Wires.SPB_HEADER_SIZE; // length of message plus length of header
            }
        } finally {
            pauser.reset();
        }
    }
}
