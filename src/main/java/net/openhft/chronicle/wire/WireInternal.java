/*
 * Copyright 2016 higherfrequencytrading.com
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
import net.openhft.chronicle.bytes.util.Compression;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.EnumInterner;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.pool.StringInterner;
import net.openhft.chronicle.core.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.List;

import static net.openhft.chronicle.wire.Wires.toIntU30;

/*
 * Created by peter.lawrey on 16/01/15.
 */
public enum WireInternal {
    ;
    static final StringInterner INTERNER = new StringInterner(128);
    static final StringBuilderPool SBP = new StringBuilderPool();
    static final StringBuilderPool ASBP = new StringBuilderPool();
    static final ThreadLocal<WeakReference<Bytes>> BYTES_TL = new ThreadLocal<>();
    static final ThreadLocal<WeakReference<Bytes>> BYTES_F2S_TL = new ThreadLocal<>();
    static final ThreadLocal<WeakReference<Wire>> BINARY_WIRE_TL = new ThreadLocal<>();
    static final ThreadLocal<WeakReference<Bytes>> ABYTES_TL = new ThreadLocal();

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

    @NotNull
    public static <E extends Enum<E>> E internEnum(@NotNull Class<E> eClass, @NotNull CharSequence cs) {
        return (E) EnumInterner.ENUM_INTERNER.get(eClass).intern(cs);
    }

    // these might be used internally so not safe for end users.
    static StringBuilder acquireStringBuilder() {
        return SBP.acquireStringBuilder();
    }

    // these might be used internally so not safe for end users.
    static StringBuilder acquireAnotherStringBuilder(CharSequence cs) {
        StringBuilder sb = ASBP.acquireStringBuilder();
        assert sb != cs;
        return sb;
    }

    public static long writeData(@NotNull WireOut wireOut, boolean metaData, boolean notComplete,
                                 @NotNull WriteMarshallable writer) {
        wireOut.getValueOut().resetBetweenDocuments();
        assert wireOut.startUse();
        long position;
        try {
            @NotNull Bytes bytes = wireOut.bytes();
            position = bytes.writePosition();

            int metaDataBit = metaData ? Wires.META_DATA : 0;
            int len0 = metaDataBit | Wires.NOT_COMPLETE | Wires.UNKNOWN_LENGTH;
            bytes.writeOrderedInt(len0);
            writer.writeMarshallable(wireOut);
            long position1 = bytes.writePosition();
//            if (position1 < position)
//                System.out.println("Message truncated from " + position + " to " + position1);
            int length = metaDataBit | toIntU30(position1 - position - 4, "Document length %,d out of 30-bit int range.");
            if (!bytes.compareAndSwapInt(position, len0, length | (notComplete ? Wires.NOT_COMPLETE : 0)))
                throw new IllegalStateException("This wire was altered by more than one thread.");
        } finally {
            assert wireOut.endUse();
        }

        return position;
    }

    public static boolean readData(long offset,
                                   @NotNull WireIn wireIn,
                                   @Nullable ReadMarshallable metaDataConsumer,
                                   @Nullable ReadMarshallable dataConsumer) {
        @NotNull final Bytes bytes = wireIn.bytes();
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
        @NotNull final Bytes<?> bytes = wireIn.bytes();
        boolean read = false;
        while (bytes.readRemaining() >= 4) {
            long position = bytes.readPosition();
            int header = bytes.readVolatileInt(position);
            if (!isKnownLength(header))
                return read;
            bytes.readSkip(4);

            final int len = Wires.lengthOf(header);
            if (Wires.isData(header)) {
                if (dataConsumer == null) {
                    return false;

                } else {
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
        @NotNull final Bytes<?> bytes = wireIn.bytes();
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

    private static boolean isKnownLength(long len) {
        return (len & (Wires.META_DATA | Wires.LENGTH_MASK)) != Wires.UNKNOWN_LENGTH;
    }

    public static Throwable throwable(@NotNull ValueIn valueIn, boolean appendCurrentStack) {
        @Nullable Class type = valueIn.typePrefix();
        Throwable throwable = ObjectUtils.newInstance((Class<Throwable>) type);

        return throwable(valueIn, appendCurrentStack, throwable);
    }

    protected static Throwable throwable(@NotNull ValueIn valueIn, boolean appendCurrentStack, Throwable throwable) {
        final Throwable finalThrowable = throwable;
        @NotNull final List<StackTraceElement> stes = new ArrayList<>();
        valueIn.marshallable(m -> {
            @Nullable final String message = m.read(() -> "message").text();

            if (message != null) {
                try {
                    DETAILED_MESSAGE.set(finalThrowable, message);
                } catch (IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            }
            m.read(() -> "stackTrace").sequence(stes, (stes0, stackTrace) -> {
                while (stackTrace.hasNextSequenceItem()) {
                    stackTrace.marshallable(r -> {
                        @Nullable final String declaringClass = r.read(() -> "class").text();
                        @Nullable final String methodName = r.read(() -> "method").text();
                        @Nullable final String fileName = r.read(() -> "file").text();
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
    public static void compress(@NotNull ValueOut out, @NotNull String compression, String str) {
        Bytes bytes = Wires.acquireBytes();
        bytes.writeUtf8(str);
        Bytes bytes2 = Wires.acquireAnotherBytes();
        Compression.compress(compression, bytes, bytes2);
        out.bytes(compression, bytes2);
    }
}
