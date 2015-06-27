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
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.pool.StringInterner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
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
    private static final Field DETAILED_MESSAGE = Jvm.getField(Throwable.class, "detailMessage");
    private static final Field STACK_TRACE = Jvm.getField(Throwable.class, "stackTrace");

    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(WireSerializedLambda.class, "SerializedLambda");
        ClassAliasPool.CLASS_ALIASES.addAlias(WireType.class);
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
        long position = bytes.writePosition();
        int metaDataBit = metaData ? META_DATA : 0;
        bytes.writeOrderedInt(metaDataBit | NOT_READY | UNKNOWN_LENGTH);
        writer.accept(wireOut);
        int length = metaDataBit | toIntU30(bytes.writePosition() - position - 4, "Document length %,d out of 30-bit int range.");
        bytes.writeOrderedInt(position, length | (notReady ? NOT_READY : 0));
    }

    public static boolean readData(long offset,
                                   @NotNull WireIn wireIn,
                                   @Nullable Consumer<WireIn> metaDataConsumer,
                                   @Nullable Consumer<WireIn> dataConsumer) {
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
                                   @Nullable Consumer<WireIn> metaDataConsumer,
                                   @Nullable Consumer<WireIn> dataConsumer) {
        final Bytes bytes = wireIn.bytes();
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
                    wireIn.bytes().readWithLength(len, b -> dataConsumer.accept(wireIn));
                    return true;
                }
            } else {

                if (metaDataConsumer == null)
                    // skip the header
                    wireIn.bytes().readSkip(len);
                else
                    wireIn.bytes().readWithLength(len, b -> metaDataConsumer.accept(wireIn));

                if (dataConsumer == null)
                    return true;
                read = true;
            }
        }
        return read;
    }

    public static String fromSizePrefixedBlobs(@NotNull Bytes bytes) {
        long position = bytes.readPosition();
        return fromSizePrefixedBlobs(bytes, position, bytes.readRemaining());
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
    public static String fromSizePrefixedBlobs(@NotNull Bytes bytes, long position, long length) {
        StringBuilder sb = new StringBuilder();

        final long limit0 = bytes.readLimit();
        final long position0 = bytes.readPosition();
        try {
            bytes.readPosition(position);
            bytes.readLimit(position + length);
            while (bytes.readRemaining() >= 4) {
                long header = bytes.readUnsignedInt();
                int len = lengthOf(header);
                String type = isData(header)
                        ? isReady(header) ? "!!data" : "!!not-ready-data!"
                        : isReady(header) ? "!!meta-data" : "!!not-ready-meta-data!";
                boolean binary = bytes.readByte(bytes.readPosition()) < ' ';
                sb.append("--- ").append(type).append(binary ? " #binary\n" : "\n");
                for (int i = 0; i < len; i++) {
                    int ch = bytes.readUnsignedByte();
                    if (binary)
                        sb.append(RandomDataInput.charToString[ch]);
                    else
                        sb.append((char) ch);
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

    public static boolean isKnownLength(long len) {
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
