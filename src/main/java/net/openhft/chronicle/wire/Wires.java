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
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.pool.StringInterner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public static final StringInterner INTERNER = new StringInterner(128);

    static final StringBuilderPool SBP = new StringBuilderPool();
    static final StringBuilderPool ASBP = new StringBuilderPool();
    static final StackTraceElement[] NO_STE = {};
    private static final Field DETAILED_MESSAGE;
    private static final Field STACK_TRACE;

    static {
        try {
            DETAILED_MESSAGE = Throwable.class.getDeclaredField("detailMessage");
            DETAILED_MESSAGE.setAccessible(true);
            STACK_TRACE = Throwable.class.getDeclaredField("stackTrace");
            STACK_TRACE.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    public static StringBuilder acquireStringBuilder() {
        return SBP.acquireStringBuilder();
    }

    public static StringBuilder acquireAnotherStringBuilder(CharSequence cs) {
        StringBuilder sb = ASBP.acquireStringBuilder();
        assert sb != cs;
        return sb;
    }

    public static void writeData(@NotNull WireOut wireOut, boolean metaData, boolean notReady, @NotNull Consumer<WireOut> writer) {
        Bytes bytes = wireOut.bytes();
        long position = bytes.position();
        int metaDataBit = metaData ? META_DATA : 0;
        bytes.writeOrderedInt(metaDataBit | NOT_READY | UNKNOWN_LENGTH);
        writer.accept(wireOut);
        int length = metaDataBit | toIntU30(bytes.position() - position - 4, "Document length %,d out of 30-bit int range.");
        bytes.writeOrderedInt(position, length | (notReady ? NOT_READY : 0));
    }

    public static boolean readData(long offset,
                                   @NotNull WireIn wireIn,
                                   @Nullable Consumer<WireIn> metaDataConsumer,
                                   @Nullable Consumer<WireIn> dataConsumer) {
        final Bytes bytes = wireIn.bytes();

        long position = bytes.position();
        long limit = bytes.limit();
        try {
            bytes.limit(bytes.isElastic() ? bytes.capacity() : bytes.realCapacity());
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
            final boolean ready = isReady(header);
            final int len = lengthOf(header);
            if (isData(header)) {
                if (dataConsumer == null) {
                    return false;

                } else {
                    ((InternalWireIn) wireIn).setReady(ready);
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

    public static String fromSizePrefixedBlobs(@NotNull Bytes bytes) {
        long position = bytes.position();
        return fromSizePrefixedBlobs(bytes, position, bytes.remaining());
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

    public static boolean isData(@NotNull WireIn wireIn) {
        final Bytes bytes = wireIn.bytes();

        if (bytes.remaining() < 4)
            throw new IllegalStateException();

        long position = bytes.position();
        int header = bytes.readVolatileInt(position);
        if (!isKnownLength(header))
            throw new IllegalStateException("unknown len, header=" + header);

        return isData(header);
    }

    @NotNull
    public static String fromSizePrefixedBlobs(@NotNull Bytes bytes, long position, long length) {
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

    public static boolean isKnownLength(long len) {
        return (len & (META_DATA | LENGTH_MASK)) != UNKNOWN_LENGTH;
    }

    public static <E> E readObject(@NotNull ValueIn in, @Nullable E using, @NotNull Class<E> clazz) {
        if (byte[].class.isAssignableFrom(clazz))
            return (E) in.bytes();

        else if (Marshallable.class.isAssignableFrom(clazz)) {
            final E v;
            if (using == null)
                try {
                    v = clazz.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            else
                v = using;

            in.marshallable((Marshallable) v);
            return v;

        } else if (StringBuilder.class.isAssignableFrom(clazz)) {
            StringBuilder builder = (using == null)
                    ? acquireStringBuilder()
                    : (StringBuilder) using;
            in.textTo(builder);
            return (E) builder;

        } else if (CharSequence.class.isAssignableFrom(clazz)) {
            //noinspection unchecked
            return (E) in.text();

        } else if (Long.class.isAssignableFrom(clazz)) {
            //noinspection unchecked
            return (E) (Long) in.int64();

        } else if (Double.class.isAssignableFrom(clazz)) {
            //noinspection unchecked
            return (E) (Double) in.float64();

        } else if (Integer.class.isAssignableFrom(clazz)) {
            //noinspection unchecked
            return (E) (Integer) in.int32();

        } else if (Float.class.isAssignableFrom(clazz)) {
            //noinspection unchecked
            return (E) (Float) in.float32();

        } else if (Short.class.isAssignableFrom(clazz)) {
            //noinspection unchecked
            return (E) (Short) in.int16();

        } else if (Character.class.isAssignableFrom(clazz)) {
            //noinspection unchecked
            final String text = in.text();
            if (text == null || text.length() == 0)
                return null;
            return (E) (Character) text.charAt(0);

        } else if (Byte.class.isAssignableFrom(clazz)) {
            //noinspection unchecked
            return (E) (Byte) in.int8();

        } else if (Map.class.isAssignableFrom(clazz)) {
            //noinspection unchecked

            final HashMap result = new HashMap();
            in.map(result);
            return (E) result;

        } else {
            throw new IllegalStateException("unsupported type");
        }
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
