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
import net.openhft.chronicle.bytes.BytesComment;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.StackTrace;
import net.openhft.chronicle.core.onoes.Slf4jExceptionHandler;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.values.LongValue;
import net.openhft.chronicle.threads.Pauser;
import net.openhft.chronicle.threads.TimingPauser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.EOFException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.StreamCorruptedException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.openhft.chronicle.wire.Wires.*;

/*
 * Created by Peter Lawrey on 10/03/16.
 */
public abstract class AbstractWire implements Wire {
    protected static final boolean ASSERTIONS;
    private static final String INSIDE_HEADER_MESSAGE = "you cant put a header inside a header, check that " +
            "you have not nested the documents. If you are using Chronicle-Queue please " +
            "ensure that you have a unique instance of the Appender per thread, in " +
            "other-words you can not share appenders across threads.";
    /**
     * This code is used to stop keeping track of the index that it was just about to write, and just
     * write the data if the appender was more than 1<<20 = 1MB behind, if the appender is less
     * behind than 1MB then, it will cycle through each excerpt to keep track of the sequence number
     * ( we call this the headerNumber ). Keeping track if you are very far behind is expensive (
     * time wise ) as it has to walk up the queue, recording the number of messages it encounters to
     * know what the index number is for the message it is about to write. This is not a problem if
     * all the appenders are on the same thread, it never gets behind. So when you have a multi
     * threaded appender, we look to see how far behind your appender is compare to the end of the
     * queue which was written by another thread, if its far behind, we just write the data, and
     * don’t update the headerNumber, the code later uses the built in indexing to work out what the
     * header number is.
     * <p>
     * As such I have expose this property so that you can tune it , you want to keep the number as
     * large as possible, to a point where your write performance is no longer acceptable ( of
     * appenders that have fallen behind )
     */
    private static long ignoreHeaderCountIfNumberOfBytesBehindExceeds = Integer.getInteger
            ("ignoreHeaderCountIfNumberOfBytesBehindExceeds", 1 << 20);

    /**
     * See comments on tryMoveToEndOfQueue
     */
    private static boolean disableFastForwardHeaderNumber = Boolean.getBoolean("disableFastForwardHeaderNumber");

    static {
        boolean assertions = false;
        // enable our class assertions if java assertions are turned on
        assert assertions = true;
        ASSERTIONS = assertions;
        WireInternal.addAliases();
    }

    @NotNull
    protected final Bytes<?> bytes;
    protected final boolean use8bit;

    protected ClassLookup classLookup = ClassAliasPool.CLASS_ALIASES;
    protected Object parent;
    @Nullable
    volatile Thread usedBy;
    @Nullable
    volatile Throwable usedHere, lastEnded;
    int usedCount = 0;
    private Pauser pauser;
    private TimingPauser timedParser;
    private long headerNumber = Long.MIN_VALUE;
    private boolean notCompleteIsNotPresent;
    private ObjectOutput objectOutput;
    private ObjectInput objectInput;
    private boolean insideHeader;
    private HeadNumberChecker headNumberChecker;

    public AbstractWire(@NotNull Bytes bytes, boolean use8bit) {
        this.bytes = bytes;
        this.use8bit = use8bit;
        notCompleteIsNotPresent = bytes.sharedMemory();
    }

    private static long throwNotEnoughSpace(int maxlen, @NotNull Bytes<?> bytes) {
        throw new IllegalStateException("not enough space to write " + maxlen + " was " + bytes.writeRemaining() + " limit " + bytes.writeLimit() + " type " + bytes.getClass());
    }

    @NotNull
    private TimingPauser acquireTimedParser() {
        return timedParser != null
                ? timedParser
                : (timedParser = Pauser.timedBusy());
    }

    public boolean isInsideHeader() {
        return this.insideHeader;
    }

    @Override
    public Pauser pauser() {
        // I don't like this code below, but lots of the existing code is expecting it to work like
        // this - yuk !
        if (pauser == null)
            pauser = acquireTimedParser();
        return pauser;
    }

    @Override
    public void pauser(Pauser pauser) {
        this.pauser = pauser;
    }

    @Override
    public void clear() {
        bytes.clear();
        headerNumber(Long.MIN_VALUE);
    }

    @NotNull
    private Wire headerNumber(long position, long headerNumber) {
        assert checkHeader(position, headerNumber);
        return headerNumber0(headerNumber);
    }

    private boolean checkHeader(long position, long headerNumber) {
        return headNumberChecker == null
                || headNumberChecker.checkHeaderNumber(headerNumber, position);
    }

    @NotNull
    @Override
    public Wire headerNumber(long headerNumber) {
        return headerNumber(bytes().writePosition(), headerNumber);
    }

    @NotNull
    private Wire headerNumber0(long headerNumber) {
//        new Exception("thread: " + Thread.currentThread().getName() + "\n\tHeader number: " + Long.toHexString(headerNumber)).printStackTrace();
        this.headerNumber = headerNumber;
        return this;
    }

    public void headNumberCheck(HeadNumberChecker headNumberChecker) {
        this.headNumberChecker = headNumberChecker;
    }

    @Override
    public long headerNumber() {
        return headerNumber;
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
    public BytesComment<?> bytesComment() {
        return bytes;
    }

    @NotNull
    @Override
    public HeaderType readDataHeader(boolean includeMetaData) throws EOFException {

        for (; ; ) {
            int header = bytes.peekVolatileInt();
            if (isReady(header)) {
                if (isData(header))
                    return HeaderType.DATA;
                if (includeMetaData && isReadyMetaData(header))
                    return HeaderType.META_DATA;

                int bytesToSkip = lengthOf(header) + SPB_HEADER_SIZE;
                bytes.readSkip(bytesToSkip);
            } else {
                if (header == END_OF_DATA)
                    throw new EOFException();
                return HeaderType.NONE;
            }
        }
    }

    @Override
    public void readAndSetLength(long position) {
        int header = bytes.peekVolatileInt();
        if (isReady(header)) {
            if (header == NOT_INITIALIZED)
                throwISE();
            long start = position + SPB_HEADER_SIZE;
            bytes.readPositionRemaining(start, lengthOf(header));
            return;
        }
        throwISE();
    }

    private void throwISE() {
        throw new IllegalStateException();
    }

    @Override
    public void readMetaDataHeader() {
        int header = bytes.peekVolatileInt();
        if (isReady(header)) {
            if (header == NOT_INITIALIZED)
                throw new IllegalStateException("Meta data not initialised");
            if (isReadyMetaData(header)) {
                setLimitPosition(header);
                return;
            }
        }
        throw new IllegalStateException("Meta data not ready " + Integer.toHexString(header));
    }

    private void setLimitPosition(int header) {
        bytes.readLimit(bytes.readPosition() + lengthOf(header) + SPB_HEADER_SIZE)
                .readSkip(SPB_HEADER_SIZE);
    }

    @Override
    public void readFirstHeader(long timeout, TimeUnit timeUnit) throws TimeoutException, StreamCorruptedException {
        int header;
        try {
            for (; ; ) {
                header = bytes.readVolatileInt(0L);
                if (isReady(header)) {
                    break;
                }
                acquireTimedParser().pause(timeout, timeUnit);
            }

        } finally {
            resetTimedPauser();
        }

        int len = lengthOf(header);
        if (!isReadyMetaData(header) || len > 64 << 10)
            throw new StreamCorruptedException("Unexpected magic number " + Integer.toHexString(header));
        bytes.readPositionRemaining(SPB_HEADER_SIZE, len);
    }

    @Override
    public long writeHeaderOfUnknownLength(final int safeLength, final long timeout, final TimeUnit timeUnit,
                                           @Nullable final LongValue lastPosition, final Sequence sequence)
            throws TimeoutException, EOFException {
        assert !insideHeader : INSIDE_HEADER_MESSAGE;

        insideHeader = true;

        try {
            long tryPos = tryWriteHeader(safeLength);
            if (tryPos != TRY_WRITE_HEADER_FAILED)
                return tryPos;

            if (lastPosition != null && sequence != null)
                tryMoveToEndOfQueue(lastPosition, sequence);

            return writeHeader0(Wires.UNKNOWN_LENGTH, safeLength, timeout, timeUnit);
        } catch (Throwable t) {
            insideHeader = false;
            throw t;
        }
    }

    @Override
    public long enterHeader(int safeLength) {
        if (safeLength > bytes.writeRemaining())
            return throwNotEnoughSpace(safeLength, bytes);
        assert !insideHeader : INSIDE_HEADER_MESSAGE;

        insideHeader = true;
        long pos = bytes.writePosition();

        for (; ; ) {
            int header = bytes.readVolatileInt(pos);
            if (header == NOT_INITIALIZED)
                break;

            if (isNotComplete(header)) {
                Jvm.warn().on(getClass(), new Exception("Incomplete header found at pos: " + pos + ": " + Integer.toHexString(header) + ", overwriting"));
                bytes.writeVolatileInt(pos, NOT_INITIALIZED);
                break;
            }

            pos += lengthOf(header) + SPB_HEADER_SIZE; // length of message plus length of header
        }
        bytes.writePositionRemaining(pos + SPB_HEADER_SIZE, safeLength);
        return pos;
    }

    /**
     * The Problem this method attempts to resolve
     * <p>
     * Appenders that have not written for a while are having to scan down the queue to get to the end,
     * there was some code that attempts to resolve this by detecting if the appenders are far away from the end,
     * then just jumping tot the end, but this code loses the sequence number of the end of the queue.
     * However, because we lose the sequence number we are currently over conservative before invoking this jump to the end.
     * Because we are over conservative, apparently we are still spending a lot of time
     * (I don’t have the stats to hand ) linear scanning to the end of the queue.
     * <p>
     * So, The problem is that we don’t know the sequence number of the end of the queue atomically with the lastPosition.
     * <p>
     * This proposal attempts to resolve this :
     * <p>
     * How about we introduce another LongValue into the queues header, lets call it the seqAndLastPosition
     * that stores the sequence number in the lower bits and the ( lower bits of the address in higher bits,
     * this would be done much in the same way that we store the index, containing both the cycle and seq number ),
     * or to put it another way, this LongValue will store the end of the lastPosition in the same place that the index stores it’s cycle number.
     * <p>
     * When ever we store the lastPosition, we should also store the seqAndLastPosition.
     * <p>
     * so to get the the lastPosition and the sequence number of the approximate end of the queue, first we read the lastPosition,
     * then we read the seqAndLastPosition, if the last bits of the lastPosition, match the higher bits of seqAndLastPosition
     * we can be sure that these are atomic, however, if they don’t match we just retry, until they do match.
     * <p>
     * Hence we don’t have to any more linear scan too far down the queue or jump to the end without knowing what the sequence number of this address is !
     *
     * @param lastPosition the end of the queue
     * @param sequence     and object that can be used for getting the last sequence
     */
    private void tryMoveToEndOfQueue(@NotNull final LongValue lastPosition,
                                     @NotNull final Sequence sequence) {

        long lastPositionValue = lastPosition.getVolatileValue();

        // do we jump forward if there has been writes else where.
        if (lastPositionValue <= bytes.writePosition())
            return;

        if (headerNumber == Long.MIN_VALUE) {
            fastForwardDontWriteHeaderNumber(lastPositionValue);
            return;
        }

        try {

            int maxAttempts = 128;
            for (int attempt = 0; attempt < maxAttempts; attempt++) {

                long lastSequence = sequence.getSequence(lastPositionValue);

                if (lastSequence == Sequence.NOT_FOUND) {
                    fastForwardDontWriteHeaderNumber(lastPositionValue);
                    break;
                }

                if (lastSequence != Sequence.NOT_FOUND_RETRY) {

                    long currentSequence = sequence.toSequenceNumber(headerNumber);

                    // we are already ahead of the lastSequence
                    if (currentSequence > lastSequence)
                        break;

                    long newHeaderNumber = sequence.toIndex(headerNumber, lastSequence - 1);

                    if (!disableFastForwardHeaderNumber) {
                        headerNumber(newHeaderNumber);
                        bytes.writePosition(lastPositionValue);
                        break;
                    }
                }

                if (attempt == maxAttempts - 1) {
                    fastForwardDontWriteHeaderNumber(lastPositionValue);
                    break;
                }

                lastPositionValue = lastPosition.getVolatileValue();
            }
        } catch (Throwable e) {
            Jvm.warn().on(getClass(), e);
        }
    }

    private void fastForwardDontWriteHeaderNumber(long lastPositionValue) {
        if (lastPositionValue > bytes.writePosition() + ignoreHeaderCountIfNumberOfBytesBehindExceeds) {
            headerNumber(Long.MIN_VALUE);
            bytes.writePosition(lastPositionValue);
        }
    }

    private long tryWriteHeader(int safeLength) {
        long pos = bytes.writePosition();

        final int value = Wires.addMaskedTidToHeader(NOT_COMPLETE | Wires.UNKNOWN_LENGTH);
        if (bytes.compareAndSwapInt(pos, 0, value)) {

            if (safeLength > bytes.writeRemaining())
                return throwNotEnoughSpace(safeLength, bytes);

            bytes.writePositionRemaining(pos + SPB_HEADER_SIZE, safeLength);
//            System.out.println(Thread.currentThread()+" wpr pos: "+pos+" hdr "+headerNumber);
            return pos;
        }
        return TRY_WRITE_HEADER_FAILED;
    }

    private long writeHeader0(int length, int safeLength, long timeout, TimeUnit timeUnit) throws TimeoutException, EOFException {
        if (length < 0 || length > safeLength)
            throwISE();
        long pos = bytes.writePosition();

        resetTimedPauser();

//        System.out.println(Thread.currentThread()+" wh0 pos: "+pos+" hdr "+(int) headerNumber);
        try {
            final int value = Wires.addMaskedTidToHeader(NOT_COMPLETE | length);
            for (; ; ) {
                if (bytes.compareAndSwapInt(pos, NOT_INITIALIZED, value)) {

                    bytes.writePosition(pos + SPB_HEADER_SIZE);
                    int maxlen = length == UNKNOWN_LENGTH ? safeLength : length;
                    if (maxlen > bytes.writeRemaining())
                        throwNotEnoughSpace(maxlen, bytes);
                    bytes.writeLimit(bytes.writePosition() + maxlen);
                    return pos;
                }
                bytes.readPositionRemaining(pos, 0);

                int header = bytes.readVolatileInt(pos);
                // two states where it is unable to continue.
                if (Wires.isEndOfFile(header))
                    throw new EOFException();
                if (isNotComplete(header)) {
                    acquireTimedParser().pause(timeout, timeUnit);
                    continue;
                }

                acquireTimedParser().reset();

                int len = lengthOf(header);

                int nextHeader = lengthOf(bytes.readVolatileInt(pos + len + SPB_HEADER_SIZE));
                if (nextHeader > 1 << 10) {
                    int header2 = bytes.readVolatileInt(pos);
                    if (header2 != header) {
                        Jvm.warn().on(getClass(), "At pos: " + pos +
                                " header: " + header +
                                " header2: " + header2);
                        header = header2;
                    }
                }
                pos += len + SPB_HEADER_SIZE; // length of message plus length of header

                if (isData(header))
                    incrementHeaderNumber(pos);
//                System.out.println(Thread.currentThread()+" wh0-iter pos: "+pos+" hdr "+(int) headerNumber);

            }
        } finally {
            resetTimedPauser();
        }
    }

    @Override
    public void updateHeader(final long position, final boolean metaData, int expectedHeader) throws StreamCorruptedException {
        if (position <= 0) {
            // this should never happen so blow up
            IllegalStateException ex = new IllegalStateException("Attempt to write to position=" + position);
            Slf4jExceptionHandler.WARN.on(getClass(), "Attempt to update header at position=" + position, ex);
            throw ex;
        }

        // the reason we add padding is so that a message gets sent ( this is, mostly for queue as
        // it cant handle a zero len message )
        if (bytes.writePosition() == position + 4)
            addPadding(1);

        long pos = bytes.writePosition();

        int header = Maths.toUInt31(pos - position - 4);
        if (metaData) header |= META_DATA;
        if (header == UNKNOWN_LENGTH)
            throw new UnsupportedOperationException("Data messages of 0 length are not supported.");

        assert insideHeader;
        insideHeader = false;

        updateHeaderAssertions(position, pos, expectedHeader, header);

        bytes.writeLimit(bytes.capacity());
        if (!metaData)
            incrementHeaderNumber(position);
    }

    private void updateHeaderAssertions(long position, long pos, int expectedHeader, int header) throws StreamCorruptedException {
        if (ASSERTIONS) {
            checkNoDataAfterEnd(pos);
        }

        if (!bytes.compareAndSwapInt(position, expectedHeader, header)) {
            int currentHeader = bytes.readVolatileInt(position);
                throw new StreamCorruptedException("Data at " + position + " overwritten? Expected: " + Integer.toHexString(expectedHeader) + " was " + Integer.toHexString(currentHeader));
        }
    }

    private void checkNoDataAfterEnd(long pos) {
        if (pos <= bytes.realCapacity() - 4) {
            final int value = bytes.bytesStore().readVolatileInt(pos);
            if (value != 0) {
                String text;
                long pos0 = bytes.readPosition();
                try {
                    bytes.readPosition(pos);
                    text = bytes.toDebugString();
                } finally {
                    bytes.readPosition(pos0);
                }
                throw new IllegalStateException("Data was written after the end of the message, zero out data before rewinding " + text);
            }
        }
    }

    private void incrementHeaderNumber(long pos) {
        if (headerNumber != Long.MIN_VALUE)
            headerNumber(pos, headerNumber + 1);
    }

    @Override
    public boolean writeFirstHeader() {
        boolean cas = bytes.compareAndSwapInt(0L, 0, NOT_COMPLETE_UNKNOWN_LENGTH);
        if (cas)
            bytes.writeSkip(SPB_HEADER_SIZE);
        return cas;
    }

    @Override
    public void updateFirstHeader() {
        // header should use wire format so can add padding for cache alignment
        padToCacheAlign();
        long pos = bytes.writePosition();
        long actualLength = pos - SPB_HEADER_SIZE;
        if (actualLength >= 1 << 30)
            throw new IllegalStateException("Header too large was " + actualLength);
        int header = (int) (META_DATA | actualLength);
        if (!bytes.compareAndSwapInt(0L, NOT_COMPLETE_UNKNOWN_LENGTH, header))
            throw new IllegalStateException("Data at 0 overwritten? Expected: " + Integer.toHexString(NOT_COMPLETE_UNKNOWN_LENGTH) + " was " + Integer.toHexString(bytes.readVolatileInt(0L)));
    }

    @Override
    public void writeEndOfWire(long timeout, TimeUnit timeUnit, long lastPosition) {

        long pos = Math.max(lastPosition, bytes.writePosition());
        headerNumber = Long.MIN_VALUE;

        try {
            for (; ; ) {
                if (bytes.compareAndSwapInt(pos, 0, END_OF_DATA)) {
                    bytes.writePosition(pos + SPB_HEADER_SIZE);
                    return;
                }

                int header = bytes.readVolatileInt(pos);
                // two states where it is unable to continue.
                if (header == END_OF_DATA)
                    return; // already written.
                if (Wires.isNotComplete(header)) {
                    try {
                        acquireTimedParser().pause(timeout, timeUnit);

                    } catch (TimeoutException e) {
                        boolean success = bytes.compareAndSwapInt(pos, header, END_OF_DATA);
                        Jvm.warn().on(getClass(), "resetting header after timeout, " +
                                "header: " + Integer.toHexString(header) +
                                ", pos: " + pos +
                                ", success: " + success);
                    }
                    continue;
                }
                acquireTimedParser().reset();
                int len = lengthOf(header);
                pos += len + SPB_HEADER_SIZE; // length of message plus length of header
            }
        } finally {
            resetTimedPauser();
        }
    }

    private void resetTimedPauser() {
        if (timedParser != null)
            timedParser.reset();
    }

    @Override
    public Object parent() {
        return parent;
    }

    @Override
    public void parent(Object parent) {
        this.parent = parent;
    }

    @Override
    public boolean startUse() {
        Throwable usedHere = this.usedHere;
        Thread usedBy = this.usedBy;
        if (usedBy != Thread.currentThread() && usedBy != null) {
            throw new IllegalStateException("Used by " + usedBy + " while trying to use it in " + Thread.currentThread(), usedHere);
        }
        this.usedBy = Thread.currentThread();
        this.usedHere = new StackTrace();
        usedCount++;
        return true;
    }

    @Override
    public boolean endUse() {
        if (usedBy != Thread.currentThread()) {
            throw new IllegalStateException("Used by " + usedHere, usedHere);
        }
        if (--usedCount <= 0) {
            usedBy = null;
            usedHere = null;
            usedCount = 0;
            lastEnded = new StackTrace();
        }
        return true;
    }

    @Override
    public boolean notCompleteIsNotPresent() {
        return notCompleteIsNotPresent;
    }

    @Override
    public void notCompleteIsNotPresent(boolean notCompleteIsNotPresent) {
        this.notCompleteIsNotPresent = notCompleteIsNotPresent;
    }

    @Override
    public ObjectOutput objectOutput() {
        if (objectOutput == null)
            objectOutput = new WireObjectOutput(this);
        return objectOutput;
    }

    @Override
    public ObjectInput objectInput() {
        if (objectInput == null)
            objectInput = new WireObjectInput(this);
        return objectInput;
    }

    @Override
    public long readEventNumber() {
        return Long.MIN_VALUE;
    }

    /**
     * used by write bytes when doing a rollback
     */
    public void forceNotInsideHeader() {
        insideHeader = false;
    }
}
