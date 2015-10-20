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
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.annotation.ForceInline;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import org.jetbrains.annotations.NotNull;

/**
 * Created by peter on 31/08/15.
 */
public enum Wires {
    ;
    public static final int LENGTH_MASK = -1 >>> 2;
    static final StringBuilderPool SBP = new StringBuilderPool();
    public static final int NOT_READY = 1 << 31;
    public static final int META_DATA = 1 << 30;
    public static final int UNKNOWN_LENGTH = 0x0;
    public static final int NOT_INITIALIZED = 0x0;

    /**
     * This decodes some Bytes where the first 4-bytes is the length.  e.g. Wire.writeDocument wrote it.
     * <a href="https://github.com/OpenHFT/RFC/tree/master/Size-Prefixed-Blob">Size Prefixed Blob</a>
     *
     * @param bytes to decode
     * @return as String
     */
    public static String fromSizePrefixedBlobs(@NotNull Bytes bytes) {
        long position = bytes.readPosition();
        return WireInternal.fromSizePrefixedBlobs(bytes, position, bytes.readRemaining());
    }

    public static StringBuilder acquireStringBuilder() {
        return SBP.acquireStringBuilder();
    }

    public static int lengthOf(long len) {
        return (int) (len & LENGTH_MASK);
    }

    public static boolean isReady(long len) {
        return (len & NOT_READY) == 0;
    }

    public static boolean isData(long len) {
        return (len & META_DATA) == 0;
    }

    public static boolean isKnownLength(int len) {
        return (len & (META_DATA | LENGTH_MASK)) != UNKNOWN_LENGTH;
    }

    public static boolean isNotInitialized(int len) {
        return len == NOT_INITIALIZED;
    }

    public static int toIntU30(long l, @NotNull String error) {
        if (l < 0 || l > LENGTH_MASK)
            throw new IllegalStateException(String.format(error, l));
        return (int) l;
    }

    public static boolean acquireLock(BytesStore store, long position) {
        return store.compareAndSwapInt(position, NOT_INITIALIZED, NOT_READY);
    }

    public static boolean exceedsMaxLength(long length) {
        return length > LENGTH_MASK;
    }

    @ForceInline
    public static <T extends ReadMarshallable> long readData(
        @NotNull WireIn wireIn,
        @NotNull T reader) {

        // We assume that check on data readiness and type has been done by the
        // caller
        return rawRead(wireIn, reader);
    }

    @ForceInline
    public static <T extends WriteMarshallable> long writeData(
        @NotNull WireOut wireOut,
        @NotNull T writer) {

        WireInternal.writeData(wireOut, false, false, writer);

        return wireOut.bytes().writePosition();
    }

    @ForceInline
    public static <T extends WriteMarshallable> long writeMeta(
        @NotNull WireOut wireOut,
        @NotNull T writer) {

        WireInternal.writeData(wireOut, true, false, writer);

        return wireOut.bytes().writePosition();
    }

    @ForceInline
    public static <T extends ReadMarshallable> long readMeta(
        @NotNull WireIn wireIn,
        @NotNull T reader) {

        // We assume that check on meta-data readiness and type has been done by
        // the caller
        return rawRead(wireIn, reader);
    }

    @ForceInline
    static long rawRead(@NotNull WireIn wireIn, @NotNull ReadMarshallable dataConsumer) {

        final Bytes<?> bytes = wireIn.bytes();
        final int header = bytes.readVolatileInt(bytes.readPosition());
        final int len = Wires.lengthOf(header);

        bytes.readSkip(4);

        final long limit0 = bytes.readLimit();
        final long limit = bytes.readPosition() + (long) len;
        try {
            bytes.readLimit(limit);
            dataConsumer.readMarshallable(wireIn);
        } finally {
            bytes.readLimit(limit0);
            bytes.readPosition(limit);
        }

        return bytes.readPosition();
    }
}
