/*
 * Copyright 2016-2025 chronicle.software
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
import net.openhft.chronicle.wire.domestic.InternalWire;
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
 * Base implementation used by most {@link Wire} variants.
 * Maintains shared state such as {@link ClassLookup}, {@link Pauser}, padding
 * and header tracking.
 */
public abstract class AbstractWire implements Wire, InternalWire {

    /**
     * Default padding configuration from the system property {@code wire.usePadding}.
     * Padding can be used for data alignment.
     */
    public static final boolean DEFAULT_USE_PADDING = Jvm.getBoolean("wire.usePadding", false);

    /**
     * Error message produced when a header is found within another header.
     */
    private static final String INSIDE_HEADER_MESSAGE = "you cant put a header inside a header, check that " +
            "you have not nested the documents. If you are using Chronicle-Queue please " +
            "ensure that you have a unique instance of the Appender per thread, in " +
            "other-words you can not share appenders across threads.";

    static {
        // early registration of common class aliases used by all wires
        WireInternal.addAliases();
    }

    /** underlying bytes store used for all read/write operations */
    @NotNull
    protected final Bytes<?> bytes;
    /** true if 8-bit encoding is permitted */
    protected final boolean use8bit;
    /** lookup of class aliases, defaults to {@link ClassAliasPool#CLASS_ALIASES} */
    protected ClassLookup classLookup = ClassAliasPool.CLASS_ALIASES;
    /** optional parent of this wire */
    protected Object parent;
    /** listener for any comments encountered */
    protected Consumer<CharSequence> commentListener = IgnoringConsumer.IGNORING_CONSUMER;
    /** pauser returned from {@link #pauser()} */
    private Pauser pauser;
    /** lazily created timed pauser used for read timeouts */
    private TimingPauser timedParser;
    /** last header number written or read */
    private long headerNumber = Long.MIN_VALUE;
    /** treat NOT_COMPLETE as not present when reading */
    private boolean notCompleteIsNotPresent;
    /** lazy ObjectOutput used by marshalling */
    private ObjectOutput objectOutput;
    /** lazy ObjectInput used by unmarshalling */
    private ObjectInput objectInput;
    /** set when between enterHeader and updateHeader */
    private boolean insideHeader;
    /** optional checker for header sequence consistency */
    private HeadNumberChecker headNumberChecker;
    /** whether to pad messages for alignment */
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
     * Lazily initialises the {@link TimingPauser} used for timeout handling.
     *
     * @return current {@link TimingPauser} instance
     */
    @NotNull
    private TimingPauser acquireTimedParser() {
        if (timedParser == null)
            timedParser = Pauser.timedBusy();
        return timedParser;
    }

    /**
     * Indicates whether a header has been started but not yet updated.
     * The flag is set in {@link #enterHeader(long)} and cleared in
     * {@link #updateHeader(long, boolean, int)}.
     *
     * @return {@code true} if within a header
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
     * Sets the current header number, validating via {@link #checkHeader(long, long)}.
     *
     * @param position     byte position used for validation
     * @param headerNumber value to store
     * @return this wire instance
     */
    @NotNull
    private Wire headerNumber(long position, long headerNumber) {
        assert checkHeader(position, headerNumber);
        return headerNumber0(headerNumber);
    }

    /**
     * Uses {@link HeadNumberChecker} to validate a header if one has been set.
     *
     * @param position     position of the header
     * @param headerNumber expected header number
     * @return {@code true} if the checker allows the header
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
     * Stores the given header number without performing any validation.
     *
     * @param headerNumber number to store
     * @return this wire instance
     */
    @NotNull
    private Wire headerNumber0(long headerNumber) {
        this.headerNumber = headerNumber;
        return this;
    }

    /**
     * Assigns a {@link HeadNumberChecker} used to validate header sequences.
     *
     * @param headNumberChecker strategy invoked by {@link #checkHeader(long, long)}
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

    /**
     * Peeks the next header and returns its type. If {@code includeMetaData}
     * is false, meta-data messages are skipped.
     */
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

    /**
     * Positions the underlying bytes so that the next read starts at a header
     * boundary, applying padding rules if required.
     */
    private void alignForRead(Bytes<?> bytes) {
        bytes.readPositionForHeader(usePadding);
    }

    /**
     * Reads the header at {@code position} and sets the read limit to the end
     * of that message.
     */
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

    /**
     * Helper to throw an {@link IllegalStateException} when a header is not ready.
     */
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

    /**
     * Adjusts the read limit to the end of the current message and skips the header bytes.
     */
    private void setLimitPosition(int header) {
        bytes.readLimit(bytes.readPosition() + lengthOf(header) + SPB_HEADER_SIZE)
                .readSkip(SPB_HEADER_SIZE);
    }

    /**
     * Reads the first header of a data stream. The call expects the header to be
     * ready and throws {@link StreamCorruptedException} if not.
     */
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

    /**
     * Version of {@link #readFirstHeader()} that waits up to the given timeout
     * for the first header to become ready.
     */
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

    /**
     * Reserves space for a new message header and returns its position. The
     * header is marked {@code NOT_INITIALIZED} and {@link #isInsideHeader()} is
     * set to true.
     */
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

    /**
     * Writes the final header length at {@code position} and clears the inside-header state.
     * Uses CAS against {@code expectedHeader} to detect concurrent modification.
     */
    @Override
    public void updateHeader(final long headerPos, final boolean isMetaData, int templateHeader) throws StreamCorruptedException {
        if (headerPos <= 0)
            invalidPosition(headerPos);

        // the reason we add padding is so that a message gets sent ( this is, mostly for queue as
        // it cant handle a zero len message )
        long pos = bytes.writePosition();
        if (pos == headerPos + 4) {
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

        final long value = pos - headerPos - 4;
        int header = (int) value;
        if (isMetaData) header |= META_DATA;
        // shouldn't happen due to padding above.
//        assert header == UNKNOWN_LENGTH;

        assert insideHeader;
        insideHeader = false;

        assert checkNoDataAfterEnd(pos);

        if (!bytes.compareAndSwapInt(headerPos, templateHeader, header)) {
            unexpectedValue(headerPos, templateHeader);
        }

        bytes.writeLimit(bytes.capacity());
        if (!isMetaData)
            incrementHeaderNumber(headerPos);
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

    /**
     * CAS based initial write of the stream header.
     */
    @Override
    public boolean writeFirstHeader() {
        boolean cas = bytes.compareAndSwapInt(0L, 0, NOT_COMPLETE_UNKNOWN_LENGTH);
        if (cas)
            bytes.writeSkip(SPB_HEADER_SIZE);
        return cas;
    }

    /**
     * Writes the length of the first header using the current write position.
     */
    @Override
    public void updateFirstHeader() {
        // header should use wire format so can add padding for cache alignment
        padToCacheAlign();
        long pos = bytes.writePosition();
        updateFirstHeader(pos);
    }

    /**
     * Updates the first header with a specific end position.
     */
    @Override
    public void updateFirstHeader(long headerEndPos) {
        long actualLength = headerEndPos - SPB_HEADER_SIZE;
        if (actualLength >= 1 << 30)
            throw new IllegalStateException("Header too large was " + actualLength);
        int header = (int) (META_DATA | actualLength);
        if (!bytes.compareAndSwapInt(0L, NOT_COMPLETE_UNKNOWN_LENGTH, header))
            throw new IllegalStateException("Data at 0 overwritten? Expected: " + Integer.toHexString(NOT_COMPLETE_UNKNOWN_LENGTH) + " was " + Integer.toHexString(bytes.readVolatileInt(0L)));
    }

    /**
     * Writes an END_OF_DATA marker at the end of the wire if possible.
     */
    @Override
    public boolean writeEndOfWire(long timeout, TimeUnit timeoutUnit, long lastPosition) {
        return endOfWire(true, timeout, timeoutUnit, lastPosition) == EndOfWire.PRESENT_AFTER_UPDATE;
    }

    /**
     * Detects whether the wire has reached the END_OF_DATA marker, optionally writing one.
     */
    @Override
    public EndOfWire endOfWire(boolean shouldWriteEof, long timeout, TimeUnit timeoutUnit, long lastPosition) {

        long pos = Math.max(lastPosition, bytes.writePosition());
        headerNumber = Long.MIN_VALUE;

        try {
            for (; ; Jvm.nanoPause()) {
                if (usePadding)
                    pos += BytesUtil.padOffset(pos);

                if (MEMORY.safeAlignedInt(pos)) {
                    if (bytes.readVolatileInt(pos) == 0) {
                        if (!shouldWriteEof)
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
                        if (!shouldWriteEof)
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
                        if (!shouldWriteEof)
                            return EndOfWire.NOT_PRESENT;

                    try {
                        acquireTimedParser().pause(timeout, timeoutUnit);

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

    /**
     * Lazily creates and returns a standard {@link ObjectOutput} wrapper.
     */
    @Override
    public ObjectOutput objectOutput() {
        if (objectOutput == null)
            objectOutput = new WireObjectOutput(this);
        return objectOutput;
    }

    /**
     * Lazily creates and returns a standard {@link ObjectInput} wrapper.
     */
    @Override
    public ObjectInput objectInput() {
        if (objectInput == null)
            objectInput = new WireObjectInput(this);
        return objectInput;
    }

    /**
     * Default implementation returns {@link Long#MIN_VALUE}. Binary wires may override.
     */
    @Override
    public long readEventNumber() {
        return Long.MIN_VALUE;
    }

    /**
     * Clears the inside-header flag without writing a header. Used during
     * rollbacks or error recovery.
     */
    @Override
    public void forceNotInsideHeader() {
        insideHeader = false;
    }

    @Deprecated(/* to be removed as soon as is practical */)
    public void usePadding(boolean usePadding) {
        this.usePadding = usePadding;
    }

    /**
     * Returns whether message padding is currently enabled.
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
