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
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.HexDumpBytesDescription;
import net.openhft.chronicle.bytes.util.DecoratedBufferUnderflowException;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.onoes.Slf4jExceptionHandler;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.util.IgnoresEverything;
import net.openhft.chronicle.threads.Pauser;
import net.openhft.chronicle.threads.TimingPauser;
import org.jetbrains.annotations.NotNull;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.StreamCorruptedException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static net.openhft.chronicle.core.UnsafeMemory.MEMORY;
import static net.openhft.chronicle.wire.Wires.*;

/**
 * Represents the AbstractWire class which serves as a base for all Wire implementations.
 * This class provides fundamental shared behaviors, configurations, and initializations for Wire types.
 *
 * @since 2023-08-22
 */
public abstract class AbstractWire implements Wire {

    // Default padding configuration loaded from the system properties.
    public static final boolean DEFAULT_USE_PADDING = Jvm.getBoolean("wire.usePadding", false);

    // Message used when a header is detected inside another header.
    private static final String INSIDE_HEADER_MESSAGE = "you cant put a header inside a header, check that " +
            "you have not nested the documents. If you are using Chronicle-Queue please " +
            "ensure that you have a unique instance of the Appender per thread, in " +
            "other-words you can not share appenders across threads.";


    // Static block to initialize aliases for WireInternal.
    static {
        WireInternal.addAliases();
    }

    // The underlying bytes representation used by the Wire.
    @NotNull
    protected final Bytes<?> bytes;

    // Determines if the Wire uses 8-bit encoding.
    protected final boolean use8bit;

    // Provides class lookup functionalities.
    protected ClassLookup classLookup = ClassAliasPool.CLASS_ALIASES;

    // Parent object for context reference.
    protected Object parent;

    // A listener to handle comments within the Wire.
    protected Consumer<CharSequence> commentListener = IgnoringConsumer.IGNORING_CONSUMER;

    // Various internal configurations and states.
    private Pauser pauser;
    private TimingPauser timedParser;
    private long headerNumber = Long.MIN_VALUE;
    private boolean notCompleteIsNotPresent;
    private ObjectOutput objectOutput;
    private ObjectInput objectInput;
    private boolean insideHeader;
    private HeadNumberChecker headNumberChecker;
    private boolean usePadding = DEFAULT_USE_PADDING;

    /**
     * Constructor for AbstractWire.
     *
     * @param bytes   The underlying bytes representation.
     * @param use8bit Indicates if 8-bit encoding should be used.
     */
    @SuppressWarnings("rawtypes")
    protected AbstractWire(@NotNull Bytes<?> bytes, boolean use8bit) {
        this.bytes = bytes;
        this.use8bit = use8bit;
        notCompleteIsNotPresent = bytes.sharedMemory();
    }

    /**
     * Throws an IllegalStateException when there's insufficient space for writing.
     *
     * @param maxlen The maximum length required.
     * @param bytes  The underlying bytes representation.
     * @return Never returns, always throws.
     * @throws IllegalStateException If there's not enough space.
     */
    private static long throwNotEnoughSpace(long maxlen, @NotNull Bytes<?> bytes) {
        throw new IllegalStateException("not enough space to write " + maxlen + " was " + bytes.writeRemaining() + " limit " + bytes.writeLimit() + " type " + bytes.getClass());
    }

    /**
     * Acquires or initializes a timed parser.
     *
     * @return The current instance of TimingPauser.
     */
    @NotNull
    private TimingPauser acquireTimedParser() {
        if (timedParser == null)
            timedParser = Pauser.timedBusy();
        return timedParser;
    }

    /**
     * Checks if the current Wire is inside a header.
     *
     * @return True if inside a header, false otherwise.
     */
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

    /**
     * Internal method to set the header number at a specific position.
     *
     * @param position      The position in the bytes representation.
     * @param headerNumber  The header number to set.
     * @return The current Wire instance.
     */
    @NotNull
    private Wire headerNumber(long position, long headerNumber) {
        assert checkHeader(position, headerNumber);
        return headerNumber0(headerNumber);
    }

    /**
     * Checks if the header at the given position and header number is valid.
     *
     * @param position      The position in the bytes representation.
     * @param headerNumber  The header number to check.
     * @return True if the header is valid, false otherwise.
     */
    private boolean checkHeader(long position, long headerNumber) {
        return headNumberChecker == null
                || headNumberChecker.checkHeaderNumber(headerNumber, position);
    }

    @NotNull
    @Override
    public Wire headerNumber(long headerNumber) {
        return headerNumber(bytes().writePosition(), headerNumber);
    }

    /**
     * Internal method to directly set the header number.
     *
     * @param headerNumber The header number to set.
     * @return The current Wire instance.
     */
    @NotNull
    private Wire headerNumber0(long headerNumber) {
        this.headerNumber = headerNumber;
        return this;
    }

    /**
     * Sets the HeadNumberChecker instance for this Wire.
     *
     * @param headNumberChecker The HeadNumberChecker instance to set.
     */
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
    public HexDumpBytesDescription<?> bytesComment() {
        return bytes;
    }

    @Override
    public void commentListener(Consumer<CharSequence> commentListener) {
        this.commentListener = commentListener;
    }

    @NotNull
    @Override
    public HeaderType readDataHeader(boolean includeMetaData) {

        alignForRead(bytes);
        for (; ; ) {
            int header = bytes.peekVolatileInt();
//            if (isReady(header)) {
            if ((header & NOT_COMPLETE) != 0 || header == 0) {
                if (header == END_OF_DATA)
                    return HeaderType.EOF;
                return HeaderType.NONE;
            }
//                if (isData(header))
            if ((header & META_DATA) == 0)
                return HeaderType.DATA;
            if (includeMetaData && isReadyMetaData(header))
                return HeaderType.META_DATA;

            long readPosition = bytes.readPosition();
            int bytesToSkip = lengthOf(header) + SPB_HEADER_SIZE;
            readPosition += bytesToSkip;
            if (usePadding) {
                readPosition += BytesUtil.padOffset(readPosition);
            }
            bytes.readPosition(readPosition);
        }
    }

    private void alignForRead(Bytes<?> bytes) {
        // move the read position
        bytes.readPositionForHeader(usePadding);
    }

    @Override
    public void readAndSetLength(long position) {
        alignForRead(bytes);
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
        alignForRead(bytes);
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
    public void readFirstHeader() throws StreamCorruptedException {
        int header;
        if (bytes.realCapacity() >= 4) {
            header = bytes.readVolatileInt(0L);
            if (!isReady(header))
                throw new StreamCorruptedException("Not ready header is found");
            int len = lengthOf(header);
            if (!isReadyMetaData(header) || len > 64 << 10)
                throw new StreamCorruptedException("Unexpected magic number " + Integer.toHexString(header));
            bytes.readPositionRemaining(SPB_HEADER_SIZE, len);
        } else {
            throw new DecoratedBufferUnderflowException("Not enough capacity to read from");
        }
    }

    @Override
    public void readFirstHeader(long timeout, TimeUnit timeUnit) throws TimeoutException, StreamCorruptedException {
        int header;
        resetTimedPauser();
        try {
            boolean hasAtLeast4 = false;
            for (; ; ) {

                if (hasAtLeast4 || bytes.realCapacity() >= 4) {
                    hasAtLeast4 = true;
                    header = bytes.readVolatileInt(0L);
                    if (isReady(header))
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
    public long enterHeader(long safeLength) {
        if (safeLength > bytes.writeRemaining()) {
            if (bytes.isElastic()) {
                long l = bytes.writeLimit();
                Jvm.warn().on(getClass(), "Unexpected writeLimit of " + l + " capacity " + bytes.capacity());
            }
            return throwNotEnoughSpace(safeLength, bytes);
        }
        assert !insideHeader : INSIDE_HEADER_MESSAGE;

        insideHeader = true;
        long pos = bytes.writePosition();

        for (; ; ) {
            if (usePadding)
                pos += BytesUtil.padOffset(pos);

            int header = bytes.readVolatileInt(pos);
            if (header == NOT_INITIALIZED)
                break;

            if (isNotComplete(header)) {
                if (header != END_OF_DATA)
                    Jvm.warn().on(getClass(), new Exception("Incomplete header found at pos: " + pos + ": " + Integer.toHexString(header) + ", overwriting"));
                else
                    throw new WriteAfterEOFException();
                bytes.writeVolatileInt(pos, NOT_INITIALIZED);
                break;
            }

            pos += lengthOf(header) + SPB_HEADER_SIZE; // length of message plus length of header
        }
        bytes.writePositionRemaining(pos + SPB_HEADER_SIZE, safeLength);
        return pos;
    }

    @Override
    public void updateHeader(final long position, final boolean metaData, int expectedHeader) throws StreamCorruptedException {
        if (position <= 0)
            invalidPosition(position);

        // the reason we add padding is so that a message gets sent ( this is, mostly for queue as
        // it cant handle a zero len message )
        long pos = bytes.writePosition();
        if (pos == position + 4) {
            addPadding(1);
            pos = bytes.writePosition();
        }

        // clear up to the next 8 bytes to explicitly indicate "no more data" (8 covers int + max padding)
        // if there aren't at least 8 bytes remaining, then clear what we can (any new mapping will be 0 anyway)
        // also clears any dirty bits left by a failed writer/appender
        // does not get added to the length
        final BytesStore<?, ?> bytesStore = bytes.bytesStore();
        if (bytesStore.capacity() - pos >= 8) {
            bytesStore.writeLong(pos, 0);
        } else {
            long remain = bytesStore.capacity() - pos;
            for (int i = 0; i < remain; ++i)
                bytesStore.writeByte(pos + i, 0);
        }

        final long value = pos - position - 4;
        int header = (int) value;
        if (metaData) header |= META_DATA;
        // shouldn't happen due to padding above.
//        assert header == UNKNOWN_LENGTH;

        assert insideHeader;
        insideHeader = false;

        assert checkNoDataAfterEnd(pos);

        if (!bytes.compareAndSwapInt(position, expectedHeader, header)) {
            unexpectedValue(position, expectedHeader);
        }

        bytes.writeLimit(bytes.capacity());
        if (!metaData)
            incrementHeaderNumber(position);
    }

    /**
     * Throws an exception if an invalid position is encountered in the Wire. This method should only be called
     * if there's an unexpected attempt to write to a particular position.
     *
     * @param position The position in the bytes representation that is considered invalid.
     * @throws IllegalStateException If an attempt to write to an invalid position is detected.
     */
    private void invalidPosition(long position) {
        // this should never happen so blow up
        IllegalStateException ex = new IllegalStateException("Attempt to write to position=" + position);
        Slf4jExceptionHandler.WARN.on(getClass(), "Attempt to update header at position=" + position, ex);
        throw ex;
    }

    /**
     * Throws a StreamCorruptedException if the current header value doesn't match the expected value.
     *
     * @param position       The position in the bytes representation.
     * @param expectedHeader The expected header value.
     * @throws StreamCorruptedException If the current and expected headers don't match.
     */
    private void unexpectedValue(long position, int expectedHeader) throws StreamCorruptedException {
        int currentHeader = bytes.readVolatileInt(position);
        throw new StreamCorruptedException("Data at " + position + " overwritten? Expected: " + Integer.toHexString(expectedHeader) + " was " + Integer.toHexString(currentHeader));
    }

    /**
     * Checks that no data is written after the end of the message. If data is found, an exception is thrown.
     * This method returns true if no extra data is found after the end or if the check isn't feasible.
     *
     * @param pos The position to start the check from.
     * @return True if there's no data after the end or if the check isn't feasible.
     * @throws IllegalStateException If data is written after the end of the message.
     */
    private boolean checkNoDataAfterEnd(long pos) {
        // can't do this check without jumping back.
        if (!bytes.inside(pos, 4L))
            return true;
        if (pos <= bytes.writeLimit() - 4) {
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
        return true;
    }

    /**
     * Increments the header number by 1 if the current header number is not the minimum long value.
     *
     * @param pos The position in the bytes representation to update.
     */
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
        updateFirstHeader(pos);
    }

    @Override
    public void updateFirstHeader(long headerEndPos) {
        long actualLength = headerEndPos - SPB_HEADER_SIZE;
        if (actualLength >= 1 << 30)
            throw new IllegalStateException("Header too large was " + actualLength);
        int header = (int) (META_DATA | actualLength);
        if (!bytes.compareAndSwapInt(0L, NOT_COMPLETE_UNKNOWN_LENGTH, header))
            throw new IllegalStateException("Data at 0 overwritten? Expected: " + Integer.toHexString(NOT_COMPLETE_UNKNOWN_LENGTH) + " was " + Integer.toHexString(bytes.readVolatileInt(0L)));
    }

    @Override
    public boolean writeEndOfWire(long timeout, TimeUnit timeUnit, long lastPosition) {
        return endOfWire(true, timeout, timeUnit, lastPosition) == EndOfWire.PRESENT_AFTER_UPDATE;
    }

    @Override
    public EndOfWire endOfWire(boolean writeEOF, long timeout, TimeUnit timeUnit, long lastPosition) {

        long pos = Math.max(lastPosition, bytes.writePosition());
        headerNumber = Long.MIN_VALUE;

        try {
            for (; ; Jvm.nanoPause()) {
                if (usePadding)
                    pos += BytesUtil.padOffset(pos);

                if (MEMORY.safeAlignedInt(pos)) {
                    if (bytes.readVolatileInt(pos) == 0) {
                        if (!writeEOF)
                            return EndOfWire.NOT_PRESENT;

                        if (bytes.compareAndSwapInt(pos, 0, END_OF_DATA)) {
                            bytes.writePosition(pos + SPB_HEADER_SIZE);
                            write("EOF");
                            return EndOfWire.PRESENT_AFTER_UPDATE;
                        }
                    }

                } else {
                    // mis-aligned check, assume only one writer (best effort)
                    MEMORY.loadFence();
                    if (bytes.readInt(pos) == 0) {
                        if (!writeEOF)
                            return EndOfWire.NOT_PRESENT;

                        bytes.writeInt(pos, END_OF_DATA);
                        MEMORY.storeFence();
                        bytes.writePosition(pos + SPB_HEADER_SIZE);
                        return EndOfWire.PRESENT_AFTER_UPDATE;
                    }
                }

                int header = bytes.readVolatileInt(pos);
                // two states where it is unable to continue.
                if (header == END_OF_DATA)
                    return EndOfWire.PRESENT;
                if (Wires.isNotComplete(header)) {
                    if (!writeEOF)
                        return EndOfWire.NOT_PRESENT;

                    try {
                        acquireTimedParser().pause(timeout, timeUnit);

                    } catch (TimeoutException e) {
                        boolean success = bytes.compareAndSwapInt(pos, header, END_OF_DATA);
                        if (success) {
                            bytes.writePosition(pos + SPB_HEADER_SIZE);
                            write("EOF");
                        }
                        Jvm.warn().on(getClass(), "resetting header after timeout, " +
                                "header: " + Integer.toHexString(header) +
                                ", pos: " + pos +
                                ", success: " + success);

                        // FIXME Should return something here?
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

    /**
     * Resets the timed pauser if it has been initialized.
     */
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

    /**
     * Forces the internal flag 'insideHeader' to false, indicating that the current Wire is no longer inside a header.
     */
    public void forceNotInsideHeader() {
        insideHeader = false;
    }

    /**
     * Sets the usePadding property of the Wire. Note: This method might be deprecated in future releases.
     *
     * @param usePadding A boolean indicating if padding should be used.
     */
    public void usePadding(boolean usePadding) {
        this.usePadding = usePadding;
    }

    /**
     * Gets the current state of the usePadding property.
     *
     * @return True if padding is used, false otherwise.
     */
    public boolean usePadding() {
        return usePadding;
    }

    /**
     * An enumeration of consumers that ignore all calls.
     * Primarily used for scenarios where a no-op implementation is needed.
     */
    private enum IgnoringConsumer implements Consumer<CharSequence>, IgnoresEverything {
        IGNORING_CONSUMER {
            @Override
            public void accept(CharSequence charSequence) {
                // method ignores all calls
            }
        }
    }
}
