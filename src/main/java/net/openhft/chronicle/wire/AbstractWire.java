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
    protected final Bytes<?> bytes;
    protected final boolean use8bit;
    protected Pauser pauser = BusyPauser.INSTANCE;
    protected ClassLookup classLookup = ClassAliasPool.CLASS_ALIASES;

    public AbstractWire(Bytes bytes, boolean use8bit) {
        this.bytes = bytes;
        this.use8bit = use8bit;
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
    public boolean readHeader() throws EOFException {
        long pos = bytes.readPosition();
        int header = bytes.readVolatileInt(pos);
        if ((header & Wires.NOT_READY) != 0) {
            if (header == Wires.NOT_READY_UNKNOWN_LENGTH)
                throw new EOFException();
            return false;
        }

        int len = Wires.lengthOf(header) + Wires.SPB_HEADER_SIZE;
        bytes.readLimit(pos + len);
        bytes.readSkip(4);
        return true;
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
        bytes.readLimit(len + 4);
        bytes.readSkip(4);
    }


    @Override
    public long writeHeader(int length, long timeout, TimeUnit timeUnit) throws TimeoutException, EOFException {
        if (length < 0 || length > Wires.MAX_LENGTH)
            throw new IllegalArgumentException();
        long pos = bytes.writePosition();
        try {
            for (; ; ) {
                if (bytes.compareAndSwapInt(pos, 0, Wires.NOT_READY | length)) {
                    bytes.writePosition(pos + Wires.SPB_HEADER_SIZE);
                    int maxlen = length == Wires.UNKNOWN_LENGTH ? Wires.MAX_LENGTH : length;
                    if (maxlen > bytes.writeRemaining())
                        throw new IllegalStateException("not enough space to write " + maxlen + " was " + bytes.writeRemaining());
                    bytes.writeLimit(bytes.writePosition() + maxlen);
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
        int expectedHeader = Wires.NOT_READY | length;
        long pos = bytes.writePosition();
        int actualLength = Maths.toUInt31(pos - position - 4);
        if (length == Wires.UNKNOWN_LENGTH) {
            length = actualLength;
        } else if (length < actualLength)
            throw new StreamCorruptedException("Wrote " + actualLength + " when " + length + " was set initially.");
        int header = length;
        if (metaData) header |= Wires.META_DATA;
        if (!bytes.compareAndSwapInt(position, expectedHeader, header))
            throw new StreamCorruptedException("Data at " + position + " overwritten? Expected: " + Integer.toHexString(expectedHeader) + " was " + Integer.toHexString(bytes.readVolatileInt(position)));
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
