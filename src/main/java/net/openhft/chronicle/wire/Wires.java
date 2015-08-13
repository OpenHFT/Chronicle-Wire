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
import net.openhft.chronicle.bytes.RandomDataInput;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.EnumInterner;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.pool.StringInterner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.List;

import static net.openhft.chronicle.wire.BinaryWire.toIntU30;

/**
 * Created by peter.lawrey on 16/01/15.
 */
public enum Wires {
    ;
    public static final int LENGTH_MASK = -1 >>> 2;
    public static final StringInterner INTERNER = new StringInterner(128);
    static final StringBuilderPool SBP = new StringBuilderPool();
    static final StringBuilderPool ASBP = new StringBuilderPool();
    static final StackTraceElement[] NO_STE = {};
    private static final int NOT_READY = 1 << 31;
    private static final int META_DATA = 1 << 30;
    private static final int UNKNOWN_LENGTH = 0x0;
    private static final Field DETAILED_MESSAGE = Jvm.getField(Throwable.class, "detailMessage");
    private static final Field STACK_TRACE = Jvm.getField(Throwable.class, "stackTrace");

    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(WireSerializedLambda.class, "SerializedLambda");
        ClassAliasPool.CLASS_ALIASES.addAlias(WireType.class);
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

    public static void writeData(@NotNull WireOut wireOut, boolean metaData, boolean notReady, @NotNull WriteMarshallable writer) {
        Bytes bytes = wireOut.bytes();
        long position = bytes.writePosition();
        int metaDataBit = metaData ? META_DATA : 0;
        bytes.writeOrderedInt(metaDataBit | NOT_READY | UNKNOWN_LENGTH);
        writer.writeMarshallable(wireOut);
        int length = metaDataBit | toIntU30(bytes.writePosition() - position - 4, "Document length %,d out of 30-bit int range.");
        bytes.writeOrderedInt(position, length | (notReady ? NOT_READY : 0));
    }

    public static void writeDataOnce(@NotNull WireOut wireOut, boolean metaData, @NotNull WriteMarshallable writer) {
        Bytes bytes = wireOut.bytes();
        long position = bytes.writePosition();
        int metaDataBit = metaData ? META_DATA : 0;
        int value = metaDataBit | NOT_READY | UNKNOWN_LENGTH;
        if (!bytes.compareAndSwapInt(position, 0, value))
            return;
        bytes.writeSkip(4);
        writer.writeMarshallable(wireOut);
        int length = metaDataBit | toIntU30(bytes.writePosition() - position - 4, "Document length %,d out of 30-bit int range.");
        if (!bytes.compareAndSwapInt(position, value, length | META_DATA))
            throw new AssertionError();
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
            final boolean ready = isReady(header);
            final int len = lengthOf(header);
            if (isData(header)) {
                if (dataConsumer == null) {
                    return false;

                } else {
                    ((InternalWireIn) wireIn).setReady(ready);
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
        assert isReady(header) && isData(header);
        final int len = lengthOf(header);

        long limit0 = bytes.readLimit();
        long limit = bytes.readPosition() + (long) len;
        try {
            bytes.readLimit(limit);
            dataConsumer.readMarshallable(wireIn);
        } finally {
            bytes.readLimit(limit0);
        }
    }

    public static String fromSizePrefixedBlobs(@NotNull Bytes bytes) {
        long position = bytes.readPosition();
        return fromSizePrefixedBlobs(bytes, position, bytes.readRemaining());
    }

    public static String fromSizePrefixedBinaryToText(@NotNull Bytes bytes) {
        long position = bytes.readPosition();
        return fromSizePrefixedBinaryToText(bytes, position, bytes.readRemaining());
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

    @NotNull
    private static String fromSizePrefixedBlobs(@NotNull Bytes bytes, long position, long length) {
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
                int len = lengthOf(header);
                String type = isData(header)
                        ? isReady(header) ? "!!data" : "!!not-ready-data!"
                        : isReady(header) ? "!!meta-data" : "!!not-ready-meta-data!";
                boolean binary = bytes.readByte(bytes.readPosition()) < ' ';

                sb.append("--- ").append(type).append(binary ? " #binary" : "");
                if (missing > 0)
                    sb.append(" # missing: ").append(missing);
                if (len > bytes.readRemaining())
                    sb.append(" # len: ").append(len).append(", remaining: ").append(bytes.readRemaining());
                sb.append("\n");
                try {
                    for (int i = 0; i < len; i++) {
                        int ch = bytes.readUnsignedByte();
                        if (binary)
                            sb.append(RandomDataInput.charToString[ch]);
                        else
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

    @NotNull
    private static String fromSizePrefixedBinaryToText(@NotNull Bytes bytes, long position, long length) {
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
                int len = lengthOf(header);
                String type = isData(header)
                        ? isReady(header) ? "!!data" : "!!not-ready-data!"
                        : isReady(header) ? "!!meta-data" : "!!not-ready-meta-data!";
                boolean binary = bytes.readByte(bytes.readPosition()) < ' ';

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

                    try {
                        bytes.readLimit(bytes.readPosition() + len);
                        new BinaryWire(bytes).copyTo(textWire);
                    } finally {
                        bytes.readLimit(readLimit);
                    }
                    textBytes = bytes2;
                    len = (int) textBytes.readRemaining();
                }
                try {
                    for (int i = 0; i < len; i++) {
                        int ch = textBytes.readUnsignedByte();
//                        if (binary)
//                            sb.append(RandomDataInput.charToString[ch]);
//                        else
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
        return (len & (META_DATA | LENGTH_MASK)) != UNKNOWN_LENGTH;
    }

    public static Throwable throwable(@NotNull ValueIn valueIn, boolean appendCurrentStack) {
        StringBuilder type = Wires.acquireStringBuilder();
        valueIn.type(type);
        String preMessage = null;
        Throwable throwable;
        try {
            //noinspection unchecked
            throwable = OS.memory().allocateInstance((Class<Throwable>) Class.forName(INTERNER.intern(type)));
        } catch (ClassNotFoundException e) {
            preMessage = type.toString();
            throwable = new RuntimeException();
        }

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
            m.read(() -> "stackTrace").sequence(stackTrace -> {
                while (stackTrace.hasNextSequenceItem()) {
                    stackTrace.marshallable(r -> {
                        final String declaringClass = r.read(() -> "class").text();
                        final String methodName = r.read(() -> "method").text();
                        final String fileName = r.read(() -> "file").text();
                        final int lineNumber = r.read(() -> "line").int32();

                        stes.add(new StackTraceElement(declaringClass, methodName,
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
}
