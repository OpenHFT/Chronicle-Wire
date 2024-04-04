/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.pool.EnumInterner;
import net.openhft.chronicle.core.pool.StringInterner;
import net.openhft.chronicle.core.scoped.ScopedThreadLocal;
import net.openhft.chronicle.core.util.*;
import net.openhft.chronicle.wire.internal.FromStringInterner;
import net.openhft.chronicle.wire.internal.VanillaFieldInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.nio.BufferUnderflowException;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;
import static net.openhft.chronicle.wire.Wires.toIntU30;

@SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
public enum WireInternal {
    ; // none
    static final StringInterner INTERNER = new StringInterner(Jvm.getInteger("wire.interner.size", 4096));
    private static final int BINARY_WIRE_SCOPED_INSTANCES_PER_THREAD = Jvm.getInteger("chronicle.wireInternal.pool.binaryWire.instancesPerThread", 4);
    private static final int BYTES_SCOPED_INSTANCES_PER_THREAD = Jvm.getInteger("chronicle.wireInternal.pool.bytes.instancesPerThread", 2);
    static final ThreadLocal<WeakReference<Bytes<?>>> BYTES_TL = new ThreadLocal<>();
    static final ThreadLocal<WeakReference<Bytes<?>>> BYTES_F2S_TL = new ThreadLocal<>();
    static final ThreadLocal<WeakReference<Wire>> BINARY_WIRE_TL = new ThreadLocal<>();
    static final ThreadLocal<WeakReference<Bytes<?>>> INTERNAL_BYTES_TL = new ThreadLocal<>();
    static final ScopedThreadLocal<Wire> BINARY_WIRE_SCOPED_TL = new ScopedThreadLocal<>(
            () -> new BinaryWire(Wires.unmonitoredDirectBytes())
                    .setOverrideSelfDescribing(true),
            Wire::clear,
            BINARY_WIRE_SCOPED_INSTANCES_PER_THREAD);
    static final ScopedThreadLocal<Bytes<?>> BYTES_SCOPED_THREAD_LOCAL = new ScopedThreadLocal<>(
            Wires::unmonitoredDirectBytes,
            Bytes::clear,
            BYTES_SCOPED_INSTANCES_PER_THREAD);

    static final StackTraceElement[] NO_STE = {};
    static final Set<Class> INTERNABLE = new HashSet<>(Arrays.asList(
            String.class,
//            Date.class,
//            TimeZone.class,
            UUID.class,
            DayOfWeek.class,
            LocalDate.class,
            LocalDateTime.class,
            LocalTime.class,
            Month.class,
            MonthDay.class,
            OffsetDateTime.class,
            OffsetTime.class,
            Period.class,
            Year.class,
            YearMonth.class,
            ZonedDateTime.class
//            ZoneId.class,
//            ZoneOffset.class
    ));
    static final Map<Class, ObjectInterner> OBJECT_INTERNERS = new ConcurrentHashMap<>();
    private static final Field DETAILED_MESSAGE = Jvm.getField(Throwable.class, "detailMessage");
    private static final Field STACK_TRACE = Jvm.getField(Throwable.class, "stackTrace");

    static {
        CLASS_ALIASES.addAlias(WireSerializedLambda.class, "SerializedLambda");
        CLASS_ALIASES.addAlias(WireType.class);
        CLASS_ALIASES.addAlias(SerializableFunction.class, "Function");
        CLASS_ALIASES.addAlias(SerializableBiFunction.class, "BiFunction");
        CLASS_ALIASES.addAlias(SerializableConsumer.class, "Consumer");
        CLASS_ALIASES.addAlias(SerializablePredicate.class, "Predicate");
        CLASS_ALIASES.addAlias(SerializableUpdater.class, "Updater");
        CLASS_ALIASES.addAlias(SerializableUpdaterWithArg.class, "UpdaterWithArg");
        CLASS_ALIASES.addAlias(VanillaFieldInfo.class, "FieldInfo");
        CLASS_ALIASES.addAlias(WireSerializedLambda.class, "SerializedLambda");
        CLASS_ALIASES.addAlias(INTERNABLE.stream().toArray(Class[]::new));
        CLASS_ALIASES.addAlias(LongArrayValueBitSet.class);
    }

    static void addAliases() {
        // static init block does the work.
    }

    @NotNull
    public static <E extends Enum<E>> E internEnum(@NotNull Class<E> eClass, @NotNull CharSequence cs) {
        return (E) EnumInterner.ENUM_INTERNER.get(eClass).intern(cs);
    }

    public static long writeData(@NotNull WireOut wireOut, boolean metaData, boolean notComplete,
                                 @NotNull WriteMarshallable writer) throws InvalidMarshallableException {
        wireOut.getValueOut().resetBetweenDocuments();
        long position;

        @NotNull Bytes<?> bytes = wireOut.bytes();
        position = bytes.writePositionForHeader(wireOut.usePadding());

        int metaDataBit = metaData ? Wires.META_DATA : 0;
        int len0 = metaDataBit | Wires.NOT_COMPLETE | Wires.UNKNOWN_LENGTH;
        bytes.writeOrderedInt(len0);
        writer.writeMarshallable(wireOut);
        if (!wireOut.isBinary())
            BytesUtil.combineDoubleNewline(bytes);
        long position1 = bytes.writePosition();
        if (wireOut.usePadding()) {
            int bytesToSkip = (int) ((position - position1) & 0x3);
            wireOut.addPadding(bytesToSkip);
            position1 = bytes.writePosition();
        }
//            if (position1 < position)
//                System.out.println("Message truncated from " + position + " to " + position1);
        int length;
        if (bytes instanceof HexDumpBytes) {
            // Todo: this looks suspicious. Why cast to int individually rather than use long arithmetics?
            length = metaDataBit | toIntU30((int) position1 - (int) position - 4, "Document length %,d out of 30-bit int range.");
        } else {
            length = metaDataBit | toIntU30(position1 - position - 4L, "Document length %,d out of 30-bit int range.");
        }
        if (wireOut.usePadding())
            bytes.testAndSetInt(position, len0, length | (notComplete ? Wires.NOT_COMPLETE : 0));
        else
            bytes.writeInt(position, length | (notComplete ? Wires.NOT_COMPLETE : 0));

        return position;
    }

    public static boolean readData(long offset,
                                   @NotNull WireIn wireIn,
                                   @Nullable ReadMarshallable metaDataConsumer,
                                   @Nullable ReadMarshallable dataConsumer) throws InvalidMarshallableException {
        @NotNull final Bytes<?> bytes = wireIn.bytes();
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
                                   @Nullable ReadMarshallable dataConsumer) throws InvalidMarshallableException {
        @NotNull final Bytes<?> bytes = wireIn.bytes();
        boolean read = false;
        while (true) {
            bytes.readPositionForHeader(wireIn.usePadding());
            if (bytes.readRemaining() < 4) break;
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
                    if (len > bytes.readRemaining())
                        throw new BufferUnderflowException();
                    long limit0 = bytes.readLimit();
                    long limit = bytes.readPosition() + len;
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

    public static void rawReadData(@NotNull WireIn wireIn, @NotNull ReadMarshallable dataConsumer) throws InvalidMarshallableException {
        @NotNull final Bytes<?> bytes = wireIn.bytes();
        int header = bytes.readInt();
        assert Wires.isReady(header) && Wires.isData(header);
        final int len = Wires.lengthOf(header);

        long limit0 = bytes.readLimit();
        long limit = bytes.readPosition() + len;
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

    public static Throwable throwable(@NotNull ValueIn valueIn, boolean appendCurrentStack) throws InvalidMarshallableException {
        @Nullable Class<?> type = valueIn.typePrefix();
        Throwable throwable = ObjectUtils.newInstance((Class<Throwable>) type);

        return throwable(valueIn, appendCurrentStack, throwable);
    }

    protected static Throwable throwable(@NotNull ValueIn valueIn, boolean appendCurrentStack, Throwable throwable) throws InvalidMarshallableException {
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

    static <T> T intern(Class<T> tClass, Object o) {
        if (INTERNABLE.contains(tClass) && o instanceof String) {
            String s = (String) o;
            if (tClass == String.class)
                return (T) INTERNER.intern(s);
            ObjectInterner interner = OBJECT_INTERNERS
                    .computeIfAbsent(tClass,
                            ObjectInterner::new);
            return (T) interner.intern(s);
        }
        return ObjectUtils.convertTo(tClass, o);
    }

    static class ObjectInterner<T> extends FromStringInterner<T> {
        final Class<T> tClass;

        ObjectInterner(Class<T> tClass) {
            super(256);
            this.tClass = tClass;
        }

        @Override
        protected @NotNull T getValue(String s) throws IORuntimeException {
            return ObjectUtils.convertTo(tClass, s);
        }
    }
}
