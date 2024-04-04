/*
 * Copyright 2016-2020 https://chronicle.software
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
 * The {@code VanillaMessageHistory} class is an implementation of {@link MessageHistory} that
 * provides an array-backed history of messages.
 */
@SuppressWarnings("rawtypes")
@Deprecated(/* to be moved in x.27 to services */)
public class VanillaMessageHistory extends SelfDescribingMarshallable implements MessageHistory {

    // Maximum length for storing message history
    public static final int MESSAGE_HISTORY_LENGTH = 128;

    public static final int MAX_LENGTH = 2 + MESSAGE_HISTORY_LENGTH * 8 * 4;
    // ThreadLocal instance for storing per-thread message history instances
    private static final ThreadLocal<MessageHistory> THREAD_LOCAL =
            ThreadLocal.withInitial(() -> {
                // Create a new VanillaMessageHistory instance for the thread
                @NotNull VanillaMessageHistory veh = new VanillaMessageHistory();
                veh.addSourceDetails(true);
                return veh;
            });

    private static final boolean HISTORY_SELF_DESCRIBING = Jvm.getBoolean("history.self.describing");
    // Configuration flag to determine whether to use bytes marshallable
    private static final boolean HISTORY_AS_BYTES = Jvm.getBoolean("history.as.bytes", !HISTORY_SELF_DESCRIBING);
    private static final boolean HISTORY_WALL_CLOCK = Jvm.getBoolean("history.wall.clock");
    private boolean useBytesMarshallable = HISTORY_AS_BYTES;
    private boolean historyWallClock = HISTORY_WALL_CLOCK;

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
    public boolean addSourceDetails() {
        return addSourceDetails;
    }

    @Override
    public void reset(int sourceId, long sourceIndex) {
        sources = 1;
        sourceIdArray[0] = sourceId;
        sourceIndexArray[0] = sourceIndex;
        timings = 1;
        timingsArray[0] = nanoTime();
    }

    @Override
    public int lastSourceId() {
        return sources <= 0 ? -1 : sourceIdArray[sources - 1];
    }

    @Override
    public long lastSourceIndex() {
        return sources <= 0 ? -1 : sourceIndexArray[sources - 1];
    }

    @Override
    public int timings() {
        return timings;
    }

    @Override
    public long timing(int n) {
        return timingsArray[n];
    }

    @Override
    public int sources() {
        return sources;
    }

    @Override
    public int sourceId(int n) {
        return sourceIdArray[n];
    }

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

    @Override
    public long sourceIndex(int n) {
        return sourceIndexArray[n];
    }

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

    @Override
    public void writeMarshallable(@NotNull BytesOut<?> b) {
        if (b.canWriteDirect(MAX_LENGTH)) {
            writeMarshallableDirect(b);
        } else {
            writeMarshallable0(b);
        }
    }

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

    public void writeMarshallable0(@NotNull BytesOut<?> b) {
        BytesOut<?> bytes = b;
        bytes.writeHexDumpDescription("sources")
                .writeUnsignedByte(sources);
        for (int i = 0; i < sources; i++)
            bytes.writeInt(sourceIdArray[i]);
        for (int i = 0; i < sources; i++)
            bytes.writeLong(sourceIndexArray[i]);

        bytes.writeHexDumpDescription("timings")
                .writeUnsignedByte(timings + 1);// one more time for this output
        for (int i = 0; i < timings; i++) {
            bytes.writeLong(timingsArray[i]);
        }
        bytes.writeLong(nanoTime()); // add time for this output
        dirty = false;
    }

    /**
     * Returns the current time in nanoseconds.
     *
     * @return Current time in nanoseconds.
     */
    protected long nanoTime() {
        return historyWallClock ? CLOCK.currentTimeNanos() : System.nanoTime();
    }

    /**
     * Writes the sources information of the provided message history to the output.
     *
     * @param t    Message history instance with the source's data.
     * @param out  Output to write the sources data to.
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
     * Writes the timings information of the provided message history to the output.
     *
     * @param t    Message history instance with the timing's data.
     * @param out  Output to write the timings data to.
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
     * Retrieves a byte description if it's available, otherwise returns null.
     *
     * @param out The output for which to retrieve the byte description.
     * @return The byte description if available, otherwise null.
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
     * Adds a new source with the given ID and index to the message history.
     *
     * @param id    The ID of the source.
     * @param index The index of the source.
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
     * Adds a timing value to the message history. Throws an exception if the maximum capacity is reached.
     *
     * @param l The timing value to be added.
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

    private CharSequence toStringTimings() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < timings; i++) {
            if (i > 0)
                sb.append(',');
            if (historyWallClock)
                sb.append(' ').append(NanoTime.INSTANCE.asString(timingsArray[i]));
            else
                sb.append(timingsArray[i]);
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

    public void useBytesMarshallable(boolean useBytesMarshallable) {
        this.useBytesMarshallable = useBytesMarshallable;
    }

    public void historyWallClock(boolean historyWallClock) {
        this.historyWallClock = historyWallClock;
    }
}
