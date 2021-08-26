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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("rawtypes")
public class VanillaMessageHistory extends SelfDescribingMarshallable implements MessageHistory {
    static boolean USE_BYTES_MARSHALLABLE = Boolean.getBoolean("history.as.bytes");
    public static final int MESSAGE_HISTORY_LENGTH = 128;
    private static final ThreadLocal<MessageHistory> THREAD_LOCAL =
            ThreadLocal.withInitial(() -> {
                @NotNull VanillaMessageHistory veh = new VanillaMessageHistory();
                veh.addSourceDetails(true);
                return veh;
            });

    // true if these change have been written
    private transient boolean dirty;

    private int sources;
    private int timings;
    @NotNull
    private final int[] sourceIdArray = new int[MESSAGE_HISTORY_LENGTH];
    @NotNull
    private final long[] sourceIndexArray = new long[MESSAGE_HISTORY_LENGTH];
    @NotNull
    private final long[] timingsArray = new long[MESSAGE_HISTORY_LENGTH * 2];
    private boolean addSourceDetails = false;

    static MessageHistory getThreadLocal() {
        return THREAD_LOCAL.get();
    }

    static void setThreadLocal(MessageHistory md) {
        if (md == null)
            THREAD_LOCAL.remove();
        else
            THREAD_LOCAL.set(md);
    }

    /**
     * Whether to automatically add timestamp on read. Set this {@code false} for utilities that expect to
     * read MessageHistory without mutation
     *
     * @param addSourceDetails addSourceDetails
     */
    public void addSourceDetails(boolean addSourceDetails) {
        this.addSourceDetails = addSourceDetails;
    }

    @Override
    public void reset() {
        sources = timings = 0;
    }

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
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        Bytes<?> bytes = wire.bytes();
        if (bytes.peekUnsignedByte() == BinaryWireCode.BYTES_MARSHALLABLE) {
            bytes.readSkip(1);
            readMarshallable0(bytes);
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
        if (USE_BYTES_MARSHALLABLE) {
            assert !(wire instanceof TextWire);
            wire.bytes().writeUnsignedByte(BinaryWireCode.BYTES_MARSHALLABLE);
            writeMarshallable(wire.bytes());
        } else {
            wire.write("sources").sequence(this, this::acceptSources);
            wire.write("timings").sequence(this, this::acceptTimings);
        }
        dirty = false;
    }

    @Override
    public void readMarshallable(@NotNull BytesIn bytes) throws IORuntimeException {
        readMarshallable0(bytes);
        assert !addSourceDetails : "Bytes marshalling does not yet support addSourceDetails";
    }

    private void readMarshallable0(@NotNull BytesIn bytes) {
        sources = bytes.readUnsignedByte();
        for (int i = 0; i < sources; i++)
            sourceIdArray[i] = bytes.readInt();
        for (int i = 0; i < sources; i++)
            sourceIndexArray[i] = bytes.readLong();
        timings = bytes.readUnsignedByte();
        for (int i = 0; i < timings; i++)
            timingsArray[i] = bytes.readLong();
    }

    @Override
    public void writeMarshallable(@NotNull BytesOut b) {
        BytesOut<?> bytes = b;
        bytes.comment("sources")
                .writeUnsignedByte(sources);
        for (int i = 0; i < sources; i++)
            bytes.writeInt(sourceIdArray[i]);
        for (int i = 0; i < sources; i++)
            bytes.writeLong(sourceIndexArray[i]);

        bytes.comment("timings")
                .writeUnsignedByte(timings + 1);// one more time for this output
        for (int i = 0; i < timings; i++) {
            bytes.writeLong(timingsArray[i]);
        }
        bytes.writeLong(nanoTime()); // add time for this output
        dirty = false;
    }

    protected long nanoTime() {
        return System.nanoTime();
    }

    private static void acceptSourcesRead(VanillaMessageHistory t, ValueIn in) {
        while (in.hasNextSequenceItem()) {
            t.addSource(in.int32(), in.int64());
        }
    }

    private static void acceptTimingsRead(VanillaMessageHistory t, ValueIn in) {
        while (in.hasNextSequenceItem()) {
            t.addTiming(in.int64());
        }
    }

    private void acceptSources(VanillaMessageHistory t, ValueOut out) {
        Bytes<?> b = out.wireOut().bytes();
        for (int i = 0; i < t.sources; i++) {
            b.comment("source id & index");
            out.uint32(t.sourceIdArray[i]);
            out.int64_0x(t.sourceIndexArray[i]);
        }
    }

    private void acceptTimings(VanillaMessageHistory t, ValueOut out) {
        Bytes<?> b = out.wireOut().bytes();
        for (int i = 0; i < t.timings; i++) {
            b.comment("timing in nanos");
            out.int64(t.timingsArray[i]);
        }
        out.int64(nanoTime());
    }

    public void addSource(int id, long index) {
        sourceIdArray[sources] = id;
        sourceIndexArray[sources++] = index;
        dirty = true;
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    public void addTiming(long l) {
        if (timings >= timingsArray.length) {
            throw new IllegalStateException("Have exceeded message history size: " + this);
        }
        timingsArray[timings++] = l;
    }

    /**
     * We need a custom toString as the base class toString calls writeMarshallable which does not mutate this,
     * but will display a different result every time you toString the object as it outputs System.nanoTime
     *
     * @return String representation
     */
    @Override
    public String toString() {
        return "VanillaMessageHistory{" +
                "sources: [" + toStringSources() +
                "] timings: [" + toStringTimings() +
                "] addSourceDetails=" + addSourceDetails +
                '}';
    }

    /**
     * Override deepCopy as writeMarshallable adds a timing every time it is called. See also {@link #toString()}
     *
     * @return copy of this
     */
    @Override
    public @NotNull VanillaMessageHistory deepCopy() {
        @NotNull VanillaMessageHistory copy = super.deepCopy();
        // remove the extra timing
        copy.timingsArray[this.timings] = 0;
        copy.timings = this.timings;
        return copy;
    }

    private String toStringSources() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sources; i++) {
            if (i > 0) sb.append(',');
            sb.append(sourceIdArray[i]).append("=0x").append(Long.toHexString(sourceIndexArray[i]));
        }
        return sb.toString();
    }

    private String toStringTimings() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < timings; i++) {
            if (i > 0) sb.append(',');
            sb.append(timingsArray[i]);
        }
        return sb.toString();
    }
}
