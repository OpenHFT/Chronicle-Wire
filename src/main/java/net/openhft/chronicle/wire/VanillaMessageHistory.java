/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.bytes.util.BinaryLengthLength;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Memory;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.wire.converter.NanoTime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

import static net.openhft.chronicle.core.time.SystemTimeProvider.CLOCK;

/**
 * Array-backed {@link MessageHistory} storing source identifiers, message indices and
 * processing timestamps. Entries are kept in internal arrays up to
 * {@link #MESSAGE_HISTORY_LENGTH}. The object can be marshalled in a compact
 * binary form or as a verbose textual structure.
 */
@SuppressWarnings({"rawtypes", "deprecation"})
public class VanillaMessageHistory extends SelfDescribingMarshallable implements MessageHistory {

    /**
     * The maximum number of source/timing entries that can be stored in a single
     * {@code VanillaMessageHistory} instance.
     */
    public static final int MESSAGE_HISTORY_LENGTH = 128;

    /**
     * The maximum number of bytes this object can take when marshalled in its
     * compact binary form.
     */
    public static final int MAX_LENGTH = 2 + MESSAGE_HISTORY_LENGTH * 8 * 4;

    /**
     * The {@link ThreadLocal} instance that holds the default
     * {@code VanillaMessageHistory} for each thread, accessible via
     * {@link MessageHistory#get()}.
     */
    private static final ThreadLocal<MessageHistory> THREAD_LOCAL =
            ThreadLocal.withInitial(() -> {
                @NotNull VanillaMessageHistory veh = new VanillaMessageHistory();
                veh.addSourceDetails(true);
                return veh;
            });

    /**
     * System property ({@code history.self.describing}) flag. If true, marshalling
     * always uses the verbose self-describing format.
     */
    private static final boolean HISTORY_SELF_DESCRIBING = Jvm.getBoolean("history.self.describing");

    /**
     * System property ({@code history.as.bytes}) flag. If true (default unless
     * {@code history.self.describing} is true), this history object will attempt
     * to use its compact binary marshallable form when written to a binary wire.
     */
    private static final boolean HISTORY_AS_BYTES = Jvm.getBoolean("history.as.bytes", !HISTORY_SELF_DESCRIBING);

    /**
     * System property ({@code history.wall.clock}) flag. If true, timings use
     * {@link net.openhft.chronicle.core.time.SystemTimeProvider#currentTimeNanos()}.
     * If false (default), {@link System#nanoTime()} is used.
     */
    private static final boolean HISTORY_WALL_CLOCK = Jvm.getBoolean("history.wall.clock");

    // Instance flag mirroring the {@code history.as.bytes} property
    private boolean useBytesMarshallable = HISTORY_AS_BYTES;
    // Instance flag mirroring the {@code history.wall.clock} property
    private boolean historyWallClock = HISTORY_WALL_CLOCK;

    // Internal arrays to store source IDs
    @NotNull
    private final int[] sourceIdArray = new int[MESSAGE_HISTORY_LENGTH];

    // Array to hold source indices
    @NotNull
    private final long[] sourceIndexArray = new long[MESSAGE_HISTORY_LENGTH];

    // Array to hold timings information
    @NotNull
    private final long[] timingsArray = new long[MESSAGE_HISTORY_LENGTH * 2];

    // Consumers for accepting sources and timings, defined to avoid lambda allocations
    private final transient BiConsumer<VanillaMessageHistory, ValueOut> acceptSourcesConsumer = this::acceptSources;
    private final transient BiConsumer<VanillaMessageHistory, ValueOut> acceptTimingsConsumer = this::acceptTimings;

    // Flag to check if the current history entry is updated
    private transient boolean dirty;

    // Count of sources and timings
    private int sources;
    private int timings;

    // Flag to decide if source details should be added or not
    private boolean addSourceDetails = false;

    /**
     * Returns the thread-local instance of {@link MessageHistory}.
     *
     * @return Current thread's {@code MessageHistory} instance.
     */
    static MessageHistory getThreadLocal() {
        return THREAD_LOCAL.get();
    }

    /**
     * Sets the thread-local instance of {@link MessageHistory}.
     *
     * @param md The {@code MessageHistory} instance to be set for the current thread.
     */
    static void setThreadLocal(MessageHistory md) {
        if (md == null)
            THREAD_LOCAL.remove();
        else
            THREAD_LOCAL.set(md);
    }

    /**
     * Accepts sources from the input stream and updates the message history.
     *
     * @param t  The instance of VanillaMessageHistory to update.
     * @param in The input stream containing source data.
     */
    private static void acceptSourcesRead(VanillaMessageHistory t, ValueIn in) {
        // Read each sequence item from the input stream
        while (in.hasNextSequenceItem()) {
            // Add the source details (ID and Index) to the message history
            t.addSource(in.int32(), in.int64());
        }
    }

    /**
     * Accepts timings from the input stream and updates the message history.
     *
     * @param t  The instance of VanillaMessageHistory to update.
     * @param in The input stream containing timing data.
     */
    private static void acceptTimingsRead(VanillaMessageHistory t, ValueIn in) {
        // Read each sequence item from the input stream
        while (in.hasNextSequenceItem()) {
            // Add the timing detail to the message history
            t.addTiming(in.int64());
        }
    }

    /**
     * Sets whether the MessageHistory should automatically add a timestamp on read.
     * Useful for utilities that expect to read MessageHistory without mutation.
     *
     * @param addSourceDetails True if source details should be added, false otherwise.
     */
    public void addSourceDetails(boolean addSourceDetails) {
        this.addSourceDetails = addSourceDetails;
    }

    /** Clears all recorded entries. */
    @Override
    public void reset() {
        sources = timings = 0;
    }

    /**
     * Gets the flag that determines whether the MessageHistory should automatically
     * add a timestamp on read.
     *
     * @return True if source details are being added, false otherwise.
     */
    @Deprecated(/* to be removed in 2027 */)
    public boolean addSourceDetails() {
        return addSourceDetails;
    }

    /**
     * Initialise the history with the given source and current time.
     *
     * @param sourceId    source identifier
     * @param sourceIndex index from the calling component
     */
    @Override
    public void reset(int sourceId, long sourceIndex) {
        sources = 1;
        sourceIdArray[0] = sourceId;
        sourceIndexArray[0] = sourceIndex;
        timings = 1;
        timingsArray[0] = nanoTime();
    }

    /**
     * @return source id of the most recent entry or {@code -1} if none
     */
    @Override
    public int lastSourceId() {
        return sources <= 0 ? -1 : sourceIdArray[sources - 1];
    }

    /**
     * @return source index of the most recent entry or {@code -1} if none
     */
    @Override
    public long lastSourceIndex() {
        return sources <= 0 ? -1 : sourceIndexArray[sources - 1];
    }

    /**
     * @return number of timing entries recorded
     */
    @Override
    public int timings() {
        return timings;
    }

    /**
     * @param n index of the entry
     * @return raw timestamp value
     */
    @Override
    public long timing(int n) {
        return timingsArray[n];
    }

    /**
     * @return number of source entries recorded
     */
    @Override
    public int sources() {
        return sources;
    }

    /**
     * @param n index of the entry
     * @return source id stored at that position
     */
    @Override
    public int sourceId(int n) {
        return sourceIdArray[n];
    }

    /**
     * @return {@code true} if the recorded source ids end with the given array
     */
    @Override
    public boolean sourceIdsEndsWith(int[] sourceIds) {
        int start = sources - sourceIds.length;
        if (start < 0)
            return false;
        for (int i = 0; i < sourceIds.length; i++) {
            if (sourceId(start + i) != sourceIds[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param n index of the entry
     * @return source index stored at that position
     */
    @Override
    public long sourceIndex(int n) {
        return sourceIndexArray[n];
    }

    /**
     * Deserialises this history from a {@link WireIn}. Binary wires may hold a
     * compact representation starting with {@link BinaryWireCode#HISTORY_MESSAGE}.
     * Structured wires read 'sources' and 'timings' fields. If
     * {@link #addSourceDetails} is true the caller is appended as another hop.
     */
    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException, InvalidMarshallableException {
        Bytes<?> bytes = wire.bytes();
        if (bytes.peekUnsignedByte() == BinaryWireCode.HISTORY_MESSAGE) {
            bytes.readSkip(1);
            if (bytes.canReadDirect(MAX_LENGTH)) {
                readMarshallableDirect(bytes);
            } else {
                readMarshallable0(bytes);
            }
        } else {
            sources = 0;
            wire.read("sources").sequence(this, VanillaMessageHistory::acceptSourcesRead);
            timings = 0;
            wire.read("timings").sequence(this, VanillaMessageHistory::acceptTimingsRead);
        }
        if (addSourceDetails) {
            @Nullable Object o = wire.parent();
            if (o instanceof SourceContext) {
                @Nullable SourceContext dc = (SourceContext) o;
                addSource(dc.sourceId(), dc.index());
            }

            addTiming(nanoTime());
        }
    }

    /**
     * Serialises this history to the given wire. Uses the compact binary form
     * when {@link #useBytesMarshallable} is true and the wire is binary,
     * otherwise writes 'sources' and 'timings' sequences. Resets the dirty flag.
     */
    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        if (useBytesMarshallable && wire.isBinary()) {
            wire.bytes().writeUnsignedByte(BinaryWireCode.HISTORY_MESSAGE);
            writeMarshallable(wire.bytes());
        } else {
            wire.write("sources").sequence(this, acceptSourcesConsumer);
            wire.write("timings").sequence(this, acceptTimingsConsumer);
        }
        dirty = false;
    }

    /**
     * Reads the message history data from the provided bytes input.
     *
     * @param bytes Input bytes to read the data from.
     */
    @Override
    public void readMarshallable(@NotNull BytesIn<?> bytes) throws IORuntimeException {
        if (bytes.canReadDirect(MAX_LENGTH)) {
            readMarshallableDirect(bytes);
        } else {
            readMarshallable0(bytes);
        }
        assert !addSourceDetails : "Bytes marshalling does not yet support addSourceDetails";
    }

    /** Optimised binary deserialisation using direct memory access. */
    private void readMarshallableDirect(@NotNull BytesIn<?> bytes) {
        long addr = bytes.addressForRead(bytes.readPosition());
        long start = addr;
        Memory memory = OS.memory();
        sources = memory.readByte(addr++);
        for (int i = 0; i < sources; i++) {
            sourceIdArray[i] = memory.readInt(addr);
            addr += 4;
        }
        for (int i = 0; i < sources; i++) {
            sourceIndexArray[i] = memory.readLong(addr);
            addr += 8;
        }
        timings = memory.readByte(addr++);
        for (int i = 0; i < timings; i++) {
            timingsArray[i] = memory.readLong(addr);
            addr += 8;
        }
        bytes.readSkip(addr - start);
    }

    /** Fallback binary deserialisation when direct memory is unavailable. */
    private void readMarshallable0(@NotNull BytesIn<?> bytes) {
        sources = bytes.readUnsignedByte();  // Read the number of sources
        // Read source IDs
        for (int i = 0; i < sources; i++)
            sourceIdArray[i] = bytes.readInt();
        // Read source indexes
        for (int i = 0; i < sources; i++)
            sourceIndexArray[i] = bytes.readLong();
        timings = bytes.readUnsignedByte();  // Read the number of timings
        // Read timing values
        for (int i = 0; i < timings; i++)
            timingsArray[i] = bytes.readLong();
    }

    /**
     * Serialises to the compact binary representation.
     */
    @Override
    public void writeMarshallable(@NotNull BytesOut<?> b) {
        if (b.canWriteDirect(MAX_LENGTH)) {
            writeMarshallableDirect(b);
        } else {
            writeMarshallable0(b);
        }
    }

    /** Optimised binary serialisation using direct memory access. */
    private void writeMarshallableDirect(BytesOut<?> b) {
        long addr = b.addressForWritePosition();
        long start = addr;
        Memory memory = OS.memory();
        memory.writeByte(addr++, (byte) sources);
        for (int i = 0; i < sources; i++) {
            memory.writeInt(addr, sourceIdArray[i]);
            addr += 4;
        }
        for (int i = 0; i < sources; i++) {
            memory.writeLong(addr, sourceIndexArray[i]);
            addr += 8;
        }
        memory.writeByte(addr++, (byte) (timings + 1));
        for (int i = 0; i < timings; i++) {
            memory.writeLong(addr, timingsArray[i]);
            addr += 8;
        }
        memory.writeLong(addr, nanoTime()); // add time for this output
        addr += 8;
        b.writeSkip(addr - start);
    }

    /** Fallback binary serialisation when direct memory is unavailable. */
    public void writeMarshallable0(@NotNull BytesOut<?> b) {
        b.writeHexDumpDescription("sources")
                .writeUnsignedByte(sources);
        for (int i = 0; i < sources; i++)
            b.writeInt(sourceIdArray[i]);
        for (int i = 0; i < sources; i++)
            b.writeLong(sourceIndexArray[i]);

        b.writeHexDumpDescription("timings")
                .writeUnsignedByte(timings + 1);// one more time for this output
        for (int i = 0; i < timings; i++) {
            b.writeLong(timingsArray[i]);
        }
        b.writeLong(nanoTime()); // add time for this output
        dirty = false;
    }

    /**
     * Returns the timestamp used for new timing entries. Uses wall-clock time
     * when {@link #historyWallClock} is true, otherwise {@link System#nanoTime()}.
     */
    protected long nanoTime() {
        return historyWallClock ? CLOCK.currentTimeNanos() : System.nanoTime();
    }

    /**
     * Internal consumer for writing the {@code sources} sequence from the
     * {@code VanillaMessageHistory} during {@link #writeMarshallable(WireOut)}.
     */
    private void acceptSources(VanillaMessageHistory t, ValueOut out) {
        HexDumpBytesDescription<?> b = bytesComment(out);

        for (int i = 0; i < t.sources; i++) {
            if (b != null)
                b.writeHexDumpDescription("source id & index");  // Add a description for hex dump
            out.uint32(t.sourceIdArray[i]);
            out.int64_0x(t.sourceIndexArray[i]);
        }
    }

    /**
     * Internal consumer for writing the {@code timings} sequence during
     * {@link #writeMarshallable(WireOut)}.
     */
    private void acceptTimings(VanillaMessageHistory t, ValueOut out) {
        HexDumpBytesDescription<?> b = bytesComment(out);
        for (int i = 0; i < t.timings; i++) {
            if (b != null)
                b.writeHexDumpDescription("timing in nanos");  // Add a description for hex dump
            out.int64(t.timingsArray[i]);
        }
        if (!(out.wireOut() instanceof HashWire) && addSourceDetails)
            out.int64(nanoTime());
    }

    /**
     * Helper to obtain a {@link HexDumpBytesDescription} if available for adding comment
     * to hex dumps when the wire supports it.
     */
    @Nullable
    private HexDumpBytesDescription<?> bytesComment(ValueOut out) {
        final WireOut wireOut = out.wireOut();
        HexDumpBytesDescription<?> b = null;
        if (!(wireOut instanceof HashWire)) {
            b = wireOut.bytes();
            if (!b.retainedHexDumpDescription())
                b = null;  // Set to null if the description isn't retained
        }
        return b;
    }

    /**
     * Adds a new source entry and marks the history as dirty.
     *
     * @param id    source identifier
     * @param index source index
     */
    public void addSource(int id, long index) {
        sourceIdArray[sources] = id;
        sourceIndexArray[sources++] = index;
        dirty = true;  // Mark as modified
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Adds a timing entry. Throws {@link IllegalStateException} if the capacity is exceeded.
     */
    public void addTiming(long l) {
        // Check if the timings array is full
        if (timings >= timingsArray.length) {
            throw new IllegalStateException("Have exceeded message history size: " + this);
        }
        // Append the provided timing value to the array and increment the count
        timingsArray[timings++] = l;
    }

    /**
     * We need a custom toString as the base class toString calls writeMarshallable which does not mutate this,
     * but will display a different result every time you toString the object as it outputs System.nanoTime
     * or Wall Clock in NS if the wall.clock.message.history system property is set
     *
     * @return String representation
     */
    @Override
    public String toString() {
        return "VanillaMessageHistory { " +
                "sources: [" + toStringSources() +
                "], timings: [" + toStringTimings() +
                "], addSourceDetails=" + addSourceDetails +
                " }";
    }

    /**
     * Override deepCopy as writeMarshallable adds a timing every time it is called. See also {@link #toString()}
     *
     * @return copy of this
     */
    @Override
    public @NotNull <T> T deepCopy() throws InvalidMarshallableException {
        @NotNull VanillaMessageHistory copy = super.deepCopy();
        // remove the extra timing
        copy.timingsArray[this.timings] = 0;
        copy.timings = this.timings;
        @SuppressWarnings("unchecked")
        T copy2 = (T) copy;
        return copy2;
    }

    /** Internal helper for {@link #toString()}. */
    private CharSequence toStringSources() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sources; i++) {
            // Append comma for all items after the first
            if (i > 0) sb.append(',');
            // Append the source ID and its index in hexadecimal format
            sb.append(sourceIdArray[i]).append("=0x").append(Long.toHexString(sourceIndexArray[i]));
        }
        return sb;
    }

    /** Internal helper for {@link #toString()}. */
    private CharSequence toStringTimings() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < timings; i++) {
            if (historyWallClock) {
                sb.append(i > 0 ? ", " : " ").append(NanoTime.INSTANCE.asString(timingsArray[i]));
            } else {
                sb.append(i > 0 ? "," : "").append(timingsArray[i]);
            }
        }
        if (historyWallClock)
            sb.append(' ');
        return sb;
    }

    @Override
    public BinaryLengthLength binaryLengthLength() {
        return BinaryLengthLength.LENGTH_16BIT;
    }

    @Override
    public void doWriteHistory(DocumentContext dc) {
        final WireOut wire = dc.wire();
        final ValueOut valueOut = useBytesMarshallable
                ? wire.writeEventId(MethodReader.MESSAGE_HISTORY_METHOD_ID)
                : wire.writeEventName(MethodReader.HISTORY);
        valueOut.marshallable(this);
    }

    /** Sets whether to use the compact binary form when writing. */
    public void useBytesMarshallable(boolean useBytesMarshallable) {
        this.useBytesMarshallable = useBytesMarshallable;
    }

    /** Sets whether to use wall-clock time for new timings. */
    public void historyWallClock(boolean historyWallClock) {
        this.historyWallClock = historyWallClock;
    }
}
