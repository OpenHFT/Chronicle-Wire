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
import net.openhft.chronicle.bytes.util.Compression;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.EnumInterner;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.pool.StringInterner;
import net.openhft.chronicle.core.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.List;

import static net.openhft.chronicle.wire.Wires.toIntU30;

/**
 * Created by peter.lawrey on 16/01/15.
 */
public enum WireInternal {
    ;
    static final StringInterner INTERNER = new StringInterner(128);
    static final StringBuilderPool SBP = new StringBuilderPool();
    static final StringBuilderPool ASBP = new StringBuilderPool();
    static final ThreadLocal<Bytes> BYTES_TL = ThreadLocal.withInitial(Bytes::allocateElasticDirect);
    static final ThreadLocal<Bytes> ABYTES_TL = ThreadLocal.withInitial(Bytes::allocateElasticDirect);
    static final StackTraceElement[] NO_STE = {};
    private static final Field DETAILED_MESSAGE = Jvm.getField(Throwable.class, "detailMessage");
    private static final Field STACK_TRACE = Jvm.getField(Throwable.class, "stackTrace");

    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(WireSerializedLambda.class, "SerializedLambda");
        ClassAliasPool.CLASS_ALIASES.addAlias(WireType.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(SerializableFunction.class, "Function");
        ClassAliasPool.CLASS_ALIASES.addAlias(SerializableBiFunction.class, "BiFunction");
        ClassAliasPool.CLASS_ALIASES.addAlias(SerializableConsumer.class, "Consumer");
        ClassAliasPool.CLASS_ALIASES.addAlias(SerializablePredicate.class, "Predicate");
        ClassAliasPool.CLASS_ALIASES.addAlias(SerializableUpdater.class, "Updater");
        ClassAliasPool.CLASS_ALIASES.addAlias(SerializableUpdaterWithArg.class, "UpdaterWithArg");
    }

    public static <E extends Enum<E>> E internEnum(Class<E> eClass, CharSequence cs) {
        return (E) EnumInterner.ENUM_INTERNER.get(eClass).intern(cs);
    }

    public static StringBuilder acquireStringBuilder() {
        return SBP.acquireStringBuilder();
    }

    public static StringBuilder acquireAnotherStringBuilder(CharSequence cs) {
        StringBuilder sb = ASBP.acquireStringBuilder();
        assert sb != cs;
        return sb;
    }

    public static long writeData(@NotNull WireOut wireOut, boolean metaData, boolean notReady,
                                 @NotNull WriteMarshallable writer) {
        Bytes bytes = wireOut.bytes();
        long position = bytes.writePosition();

        int metaDataBit = metaData ? Wires.META_DATA : 0;
        bytes.writeOrderedInt(metaDataBit | Wires.NOT_READY | Wires.UNKNOWN_LENGTH);
        writer.writeMarshallable(wireOut);
        long position1 = bytes.writePosition();
        if (position1 < position)
            System.out.println("Message truncated from " + position + " to " + position1);
        int length = metaDataBit | toIntU30(position1 - position - 4, "Document length %,d out of 30-bit int range.");
        bytes.writeOrderedInt(position, length | (notReady ? Wires.NOT_READY : 0));

        return position;
    }


    /**
     * @param wireOut  the target
     * @param metaData {@code true} if meta data
     * @param writer   the write of the data
     * @return the position the data was written or -1 if the data could not be written ( in which
     * case the position is advanced,
     */
    public static long writeDataOrAdvanceIfNotEmpty(@NotNull WireOut wireOut,
                                                    boolean metaData,
                                                    @NotNull WriteMarshallable writer) {

        Bytes bytes = wireOut.bytes();
        long position = bytes.writePosition();
        int metaDataBit = metaData ? Wires.META_DATA : 0;
        int value = metaDataBit | Wires.NOT_READY | Wires.UNKNOWN_LENGTH;
        if (!bytes.compareAndSwapInt(position, 0, value)) {
            bytes.writeSkip(Wires.lengthOf(bytes.readLong(bytes.writePosition())));
            return -1;
        }

        bytes.writeSkip(4);
        writer.writeMarshallable(wireOut);
        int length = toIntU30(bytes.writePosition() - position - 4, "Document length %,d " +
                "out of 30-bit int range.");
        if (!bytes.compareAndSwapInt(position, value, length | metaDataBit))
            throw new AssertionError();
        return position;
    }


    public static boolean readData(long offset,
                                   @NotNull WireIn wireIn,
                                   @Nullable ReadMarshallable metaDataConsumer,
                                   @Nullable ReadMarshallable dataConsumer) {
        final Bytes bytes = wireIn.bytes();
        long position = bytes.readPosition();
        long limit = bytes.readLimit();
        try {
            bytes.readLimit(bytes.isElastic() ? bytes.capacity() : bytes.realCapacity());
            bytes.readPosition(offset);
            return readData(wireIn, metaDataConsumer, dataConsumer);
        } finally {
            bytes.readLimit(limit);
            bytes.readPosition(position);
        }
    }

    public static boolean readData(@NotNull WireIn wireIn,
                                   @Nullable ReadMarshallable metaDataConsumer,
                                   @Nullable ReadMarshallable dataConsumer) {
        final Bytes<?> bytes = wireIn.bytes();
        boolean read = false;
        while (bytes.readRemaining() >= 4) {
            long position = bytes.readPosition();
            int header = bytes.readVolatileInt(position);
            if (!isKnownLength(header))
                return read;
            bytes.readSkip(4);
            final boolean ready = Wires.isReady(header);
            final int len = Wires.lengthOf(header);
            if (Wires.isData(header)) {
                if (dataConsumer == null) {
                    return false;

                } else {
                    ((InternalWire) wireIn).setReady(ready);
                    bytes.readWithLength(len, b -> dataConsumer.readMarshallable(wireIn));
                    return true;
                }
            } else {

                if (metaDataConsumer == null) {
                    // skip the header
                    bytes.readSkip(len);
                } else {
                    // bytes.readWithLength(len, b -> metaDataConsumer.accept(wireIn));
                    // inlined to avoid garbage
                    if ((long) len > bytes.readRemaining())
                        throw new BufferUnderflowException();
                    long limit0 = bytes.readLimit();
                    long limit = bytes.readPosition() + (long) len;
                    try {
                        bytes.readLimit(limit);
                        metaDataConsumer.readMarshallable(wireIn);
                    } finally {
                        bytes.readLimit(limit0);
                        bytes.readPosition(limit);
                    }
                }

                if (dataConsumer == null)
                    return true;
                read = true;
            }
        }
        return read;
    }

    public static void rawReadData(@NotNull WireIn wireIn, @NotNull ReadMarshallable dataConsumer) {
        final Bytes<?> bytes = wireIn.bytes();
        int header = bytes.readInt();
        assert Wires.isReady(header) && Wires.isData(header);
        final int len = Wires.lengthOf(header);

        long limit0 = bytes.readLimit();
        long limit = bytes.readPosition() + (long) len;
        try {
            bytes.readLimit(limit);
            dataConsumer.readMarshallable(wireIn);
        } finally {
            bytes.readLimit(limit0);
        }
    }

    @Deprecated
    public static String fromSizePrefixedBinaryToText(@NotNull Bytes bytes) {
        return Wires.fromSizePrefixedBlobs(bytes);
    }

    @NotNull
    static String fromSizePrefixedBlobs(@NotNull Bytes bytes, long position, long length) {
        StringBuilder sb = new StringBuilder();

        final long limit0 = bytes.readLimit();
        final long position0 = bytes.readPosition();
        try {
            bytes.readPosition(position);
            long limit2 = Math.min(limit0, position + length);
            bytes.readLimit(limit2);
            long missing = position + length - limit2;
            while (bytes.readRemaining() >= 4) {
                long header = bytes.readUnsignedInt();
                int len = Wires.lengthOf(header);
                if (len > bytes.readRemaining())
                    throw new RuntimeException("Are you sure this was written with writeDocument and has a 4 byte size prefix, " + len + " > " + bytes.readRemaining());
                String type = Wires.isData(header)
                        ? Wires.isReady(header) ? "!!data" : "!!not-ready-data!"
                        : Wires.isReady(header) ? "!!meta-data" : "!!not-ready-meta-data!";
                byte firstByte = bytes.readByte(bytes.readPosition());
                boolean binary = firstByte < ' ' && firstByte != '\n';

                sb.append("--- ").append(type).append(binary ? " #binary" : "");
                if (missing > 0)
                    sb.append(" # missing: ").append(missing);
                if (len > bytes.readRemaining())
                    sb.append(" # len: ").append(len).append(", remaining: ").append(bytes.readRemaining());
                sb.append("\n");

                Bytes textBytes = bytes;

                if (binary) {
                    Bytes bytes2 = Bytes.elasticByteBuffer();
                    TextWire textWire = new TextWire(bytes2);
                    long readLimit = bytes.readLimit();

                    long readPosition = bytes.readPosition();
                    try {
                        bytes.readLimit(readPosition + len);
                        new BinaryWire(bytes).copyTo(textWire);
                    } catch (Exception e) {
                        bytes.readPosition(readPosition);
                        throw new IORuntimeException("Unable to parse\n" + bytes.toHexString(Integer.MAX_VALUE), e);
                    } finally {
                        bytes.readLimit(readLimit);
                    }
                    textBytes = bytes2;
                    len = (int) textBytes.readRemaining();
                }
                try {
                    for (int i = 0; i < len; i++) {
                        int ch = textBytes.readUnsignedByte();
                        sb.append((char) ch);
                    }
                } catch (Exception e) {
                    sb.append(" ").append(e);
                }
                if (sb.charAt(sb.length() - 1) != '\n')
                    sb.append('\n');
            }

            return sb.toString();
        } finally {
            bytes.readLimit(limit0);
            bytes.readPosition(position0);
        }
    }

    private static boolean isKnownLength(long len) {
        return (len & (Wires.META_DATA | Wires.LENGTH_MASK)) != Wires.UNKNOWN_LENGTH;
    }

    public static Throwable throwable(@NotNull ValueIn valueIn, boolean appendCurrentStack) {
        Class type = valueIn.typePrefix();
        String preMessage = null;
        Throwable throwable = ObjectUtils.newInstance((Class<Throwable>) type);

        final String finalPreMessage = preMessage;
        final Throwable finalThrowable = throwable;
        final List<StackTraceElement> stes = new ArrayList<>();
        valueIn.marshallable(m -> {
            final String message = merge(finalPreMessage, m.read(() -> "message").text());

            if (message != null) {
                try {
                    DETAILED_MESSAGE.set(finalThrowable, message);
                } catch (IllegalAccessException e) {
                    throw Jvm.rethrow(e);
                }
            }
            m.read(() -> "stackTrace").sequence(stes, (stes0, stackTrace) -> {
                while (stackTrace.hasNextSequenceItem()) {
                    stackTrace.marshallable(r -> {
                        final String declaringClass = r.read(() -> "class").text();
                        final String methodName = r.read(() -> "method").text();
                        final String fileName = r.read(() -> "file").text();
                        final int lineNumber = r.read(() -> "line").int32();

                        stes0.add(new StackTraceElement(declaringClass, methodName,
                                fileName, lineNumber));
                    });
                }
            });
        });

        if (appendCurrentStack) {
            stes.add(new StackTraceElement("~ remote", "tcp ~", "", 0));
            StackTraceElement[] stes2 = Thread.currentThread().getStackTrace();
            int first = 6;
            int last = Jvm.trimLast(first, stes2);
            //noinspection ManualArrayToCollectionCopy
            for (int i = first; i <= last; i++)
                stes.add(stes2[i]);
        }
        try {
            //noinspection ToArrayCallWithZeroLengthArrayArgument
            STACK_TRACE.set(finalThrowable, stes.toArray(NO_STE));
        } catch (IllegalAccessException e) {
            throw Jvm.rethrow(e);
        }
        return throwable;
    }

    @Nullable
    static String merge(@Nullable String a, @Nullable String b) {
        return a == null ? b : b == null ? a : a + " " + b;
    }

    @Deprecated
    public static void compress(ValueOut out, String compression, String str) {
        Bytes bytes = Wires.acquireBytes();
        bytes.writeUtf8(str);
        Bytes bytes2 = Wires.acquireAnotherBytes();
        Compression.compress(compression, bytes, bytes2);
        out.bytes(compression, bytes2);
    }
}
