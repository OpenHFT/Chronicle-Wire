/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static net.openhft.chronicle.wire.BinaryWire.toIntU30;

/**
 * Created by peter.lawrey on 16/01/15.
 */
public enum Wires {
    ;
    public static final int NOT_READY = 1 << 31;
    public static final int META_DATA = 1 << 30;
    public static final int UNKNOWN_LENGTH = 0x0;
    public static final int LENGTH_MASK = -1 >>> 2;

    static final StringBuilderPool SBP = new StringBuilderPool();
    static final StringBuilderPool ASBP = new StringBuilderPool();

    public static StringBuilder acquireStringBuilder() {
        return SBP.acquireStringBuilder();
    }

    public static StringBuilder acquireAnotherStringBuilder(CharSequence cs) {
        StringBuilder sb = ASBP.acquireStringBuilder();
        assert sb != cs;
        return sb;
    }


    private static Consumer<WireIn> emptyMetaDataConsumer = new Consumer<WireIn>() {
        @Override
        public void accept(WireIn wireIn) {
            // skip the meta data
        }
    };

    public static void writeData(WireOut wireOut, boolean metaData, Consumer<WireOut> writer) {
        Bytes bytes = wireOut.bytes();
        long position = bytes.position();
        int metaDataBit = metaData ? META_DATA : 0;
        bytes.writeInt(metaDataBit | NOT_READY | UNKNOWN_LENGTH);
        writer.accept(wireOut);
        int length = metaDataBit | toIntU30(bytes.position() - position - 4, "Document length %,d out of 30-bit int range.");
        bytes.writeOrderedInt(position, length);
    }

    public static boolean readData(long offset,
                                   @NotNull WireIn wireIn,
                                   @Nullable Consumer<WireIn> metaDataConsumer,
                                   @Nullable Consumer<WireIn> dataConsumer) {


        final Bytes bytes = wireIn.bytes();

        long position = bytes.position();
        long limit = bytes.limit();
        try {
            bytes.limit( bytes.isElastic() ? bytes.capacity() : bytes.realCapacity());
            bytes.position(offset);
            return readData(wireIn, metaDataConsumer, dataConsumer);
        } finally {
            bytes.limit(limit);
            bytes.position(position);
        }
    }


    public static boolean readData(@NotNull WireIn wireIn,
                                   @Nullable Consumer<WireIn> metaDataConsumer,
                                   @Nullable Consumer<WireIn> dataConsumer) {

        final Bytes bytes = wireIn.bytes();
        boolean read = false;
        while (bytes.remaining() >= 4) {
            long position = bytes.position();
            int header = bytes.readVolatileInt(position);
            if (!isKnownLength(header))
                return read;
            bytes.skip(4);
            final int len = lengthOf(header);
            if (isData(header)) {
                if (dataConsumer == null) {
                    return false;
                } else {
                    wireIn.bytes().withLength(len, b -> dataConsumer.accept(wireIn));
                    return true;
                }
            } else {

                if (metaDataConsumer == null)
                    // skip the header
                    wireIn.bytes().skip(len);
                else
                    wireIn.bytes().withLength(len, b -> metaDataConsumer.accept(wireIn));

                if (dataConsumer == null)
                    return true;
                read = true;
            }
        }
        return read;
    }

    public static String fromSizePrefixedBlobs(Bytes bytes) {
        long position = bytes.position();
        return fromSizePrefixedBlobs(bytes, position, bytes.remaining());
    }

    @NotNull
    public static String fromSizePrefixedBlobs(Bytes bytes, long position, long length) {
        StringBuilder sb = new StringBuilder();

        final long limit0 = bytes.limit();
        final long position0 = bytes.position();
        try {
            bytes.position(position);
            bytes.limit(position + length);
            while (bytes.remaining() >= 4) {
                long header = bytes.readUnsignedInt();
                int len = lengthOf(header);
                String type = isData(header)
                        ? isReady(header) ? "!!data" : "!!not-ready-data!"
                        : isReady(header) ? "!!meta-data" : "!!not-ready-meta-data!";
                sb.append("--- ").append(type).append("\n");
                for (int i = 0; i < len; i++)
                    sb.append((char) bytes.readUnsignedByte());
                if (sb.charAt(sb.length() - 1) != '\n')
                    sb.append('\n');
            }

            return sb.toString();
        } finally {
            bytes.limit(limit0);
            bytes.position(position0);
        }
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

    public static boolean isKnownLength(long len) {
        return (len & (META_DATA | LENGTH_MASK)) != UNKNOWN_LENGTH;
    }
}
