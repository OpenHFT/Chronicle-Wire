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
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.pool.StringInterner;
import net.openhft.chronicle.core.scoped.ScopedThreadLocal;
import net.openhft.chronicle.core.threads.ThreadLocalHelper;
import net.openhft.chronicle.core.util.*;
import net.openhft.chronicle.wire.internal.FromStringInterner;
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

/**
 * The WireInternal enum provides a collection of utility constants, data structures,
 * and internal configurations to support wire operations. This enum does not have any
 * direct instances (as signified by the empty enum declaration). Instead, it serves as
 * a container for static members.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public enum WireInternal {
    ; // none
    static final StringInterner INTERNER = new StringInterner(Jvm.getInteger("wire.interner.size", 4096));
    private static final int BINARY_WIRE_SCOPED_INSTANCES_PER_THREAD = Jvm.getInteger("chronicle.wireInternal.pool.binaryWire.instancesPerThread", 4);
    private static final int BYTES_SCOPED_INSTANCES_PER_THREAD = Jvm.getInteger("chronicle.wireInternal.pool.bytes.instancesPerThread", 2);
    @Deprecated(/* For removal in x.26 */)
    static final StringBuilderPool SBP = new StringBuilderPool();
    @Deprecated(/* For removal in x.26 */)
    static final StringBuilderPool ASBP = new StringBuilderPool();
    @Deprecated(/* For removal in x.26 */)
    static final StringBuilderPool SBPVI = new StringBuilderPool();
    @Deprecated(/* For removal in x.26 */)
    static final StringBuilderPool SBPVO = new StringBuilderPool();

    // Thread-local storage for various utility instances.
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

    // Empty array for stack trace elements.
    static final StackTraceElement[] NO_STE = {};

    // Collection of classes that are internable to reduce memory consumption.
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

    // Map to store and retrieve object interners for specific classes.
    static final Map<Class, ObjectInterner> OBJECT_INTERNERS = new ConcurrentHashMap<>();

    // Internal fields for obtaining detailed messages and stack traces from Throwable instances.
    private static final Field DETAILED_MESSAGE = Jvm.getField(Throwable.class, "detailMessage");
    private static final Field STACK_TRACE = Jvm.getField(Throwable.class, "stackTrace");

    static {
        // Initialization block to add aliases for various classes.
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

    /**
     * Triggers the static initialization block of this enum.
     * The actual work of adding aliases is done by the static init block.
     */
    static void addAliases() {
        // static init block does the work.
    }

    /**
     * Returns the interned instance of the given enum {@code E} that matches the specified character sequence.
     * The method ensures that repeated requests for the same enum instance return the same shared object.
     *
     * @param <E>    The enum type
     * @param eClass The class of the enum type
     * @param cs     The character sequence that represents the enum constant
     * @return The interned enum constant that matches the provided character sequence
     */
    @NotNull
    public static <E extends Enum<E>> E internEnum(@NotNull Class<E> eClass, @NotNull CharSequence cs) {
        return (E) EnumInterner.ENUM_INTERNER.get(eClass).intern(cs);
    }

    /**
     * @deprecated Use {@link Wires#acquireStringBuilderScoped()} instead
     */
    @Deprecated(/* To be removed in x.26 */)
    // these might be used internally so not safe for end users.
    static StringBuilder acquireStringBuilder() {
        return SBP.acquireStringBuilder();
    }

    /**
     * @deprecated Use {@link Wires#acquireStringBuilderScoped()} instead
     */
    @Deprecated(/* To be removed in x.26 */)
    // these might be used internally so not safe for end users.
    static StringBuilder acquireAnotherStringBuilder(CharSequence cs) {
        StringBuilder sb = ASBP.acquireStringBuilder();
        assert sb != cs;
        return sb;
    }

    /**
     * Writes data to the provided {@code wireOut} based on the given configurations and writer.
     *
     * @param wireOut    The output wire to write to
     * @param metaData   A flag indicating if meta data should be included in the write
     * @param notComplete A flag indicating if the data is not complete
     * @param writer     The writer responsible for writing the marshallable data
     * @return The position where the data was written
     * @throws InvalidMarshallableException If the provided data cannot be marshalled properly
     */
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

    /**
     * Reads data from the provided {@code wireIn} starting from the given offset, and processes
     * the data using the provided meta data and data consumers.
     *
     * @param offset            The offset to start reading from within the wire input
     * @param wireIn            The input wire to read data from
     * @param metaDataConsumer  The consumer responsible for processing meta data.
     *                          May be null if meta data processing is not required.
     * @param dataConsumer      The consumer responsible for processing the actual data.
     *                          May be null if data processing is not required.
     * @return A boolean value indicating success or failure of reading the data.
     * @throws InvalidMarshallableException If the data cannot be unmarshalled properly.
     */
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

    /**
     * Reads data from the given {@code wireIn} and processes the data using the provided
     * meta data and data consumers. This method continues to read as long as there is data
     * to read and processes the data based on its header to determine whether it's meta data
     * or actual data.
     *
     * @param wireIn            The input wire to read data from.
     * @param metaDataConsumer  The consumer responsible for processing meta data.
     *                          May be null if meta data processing is not required.
     * @param dataConsumer      The consumer responsible for processing the actual data.
     *                          May be null if data processing is not required.
     * @return A boolean value indicating whether any data was successfully read.
     *         True if data was read, false otherwise.
     * @throws InvalidMarshallableException If the data cannot be unmarshalled properly or
     *                                      there's an issue with the data structure.
     * @throws BufferUnderflowException If attempting to read more data than what's available.
     */
    public static boolean readData(@NotNull WireIn wireIn,
                                   @Nullable ReadMarshallable metaDataConsumer,
                                   @Nullable ReadMarshallable dataConsumer) throws InvalidMarshallableException {
        @NotNull final Bytes<?> bytes = wireIn.bytes();
        boolean read = false;

        // Loop to continually read data as long as there's data available
        while (true) {
            bytes.readPositionForHeader(wireIn.usePadding());

            // If less than 4 bytes remain, break from loop
            if (bytes.readRemaining() < 4) break;
            long position = bytes.readPosition();
            int header = bytes.readVolatileInt(position);

            // Check if the header's length is known
            if (!isKnownLength(header))
                return read;

            // Move the read position past the header
            bytes.readSkip(4);

            final int len = Wires.lengthOf(header);

            // If header indicates data
            if (Wires.isData(header)) {
                if (dataConsumer == null) {
                    return false;

                } else {
                    bytes.readWithLength(len, b -> dataConsumer.readMarshallable(wireIn));
                    return true;
                }
            } else {  // If header indicates metadata

                if (metaDataConsumer == null) {
                    // Skip the metadata
                    bytes.readSkip(len);
                } else {
                    // bytes.readWithLength(len, b -> metaDataConsumer.accept(wireIn));
                    // Reading meta data, setting limits to ensure correct data length
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

                // If there's no data consumer, simply return true
                if (dataConsumer == null)
                    return true;
                read = true;
            }
        }
        return read;
    }

    /**
     * Reads raw data from the given {@code wireIn} without processing metadata and uses the provided
     * {@code dataConsumer} to process this data.
     *
     * @param wireIn        The input wire from which data is read.
     * @param dataConsumer  The consumer responsible for processing the actual data.
     * @throws InvalidMarshallableException If the data cannot be unmarshalled properly or
     *                                      there's an issue with the data structure.
     */
    public static void rawReadData(@NotNull WireIn wireIn, @NotNull ReadMarshallable dataConsumer) throws InvalidMarshallableException {
        @NotNull final Bytes<?> bytes = wireIn.bytes();

        // Read the data header from the wire
        int header = bytes.readInt();

        // Ensure the header is both ready and indicates data
        assert Wires.isReady(header) && Wires.isData(header);

        // Determine the length of the data from the header
        final int len = Wires.lengthOf(header);

        long limit0 = bytes.readLimit();
        long limit = bytes.readPosition() + len;

        // Set the new reading limit and process the data, resetting the limit afterwards
        try {
            bytes.readLimit(limit);
            dataConsumer.readMarshallable(wireIn);
        } finally {
            bytes.readLimit(limit0);
        }
    }

    /**
     * Determines if the provided length value is a known length (neither metadata nor of unknown length).
     *
     * @param len The length value to be evaluated.
     * @return True if the length is known, false otherwise.
     */
    private static boolean isKnownLength(long len) {
        return (len & (Wires.META_DATA | Wires.LENGTH_MASK)) != Wires.UNKNOWN_LENGTH;
    }

    /**
     * Creates a throwable instance based on the type provided by {@code valueIn}. Optionally, this method
     * can also append the current stack trace to the throwable if {@code appendCurrentStack} is true.
     *
     * @param valueIn           The value input from which the type of the throwable is determined.
     * @param appendCurrentStack Flag indicating whether to append the current stack trace to the throwable.
     * @return A throwable instance of the type determined from {@code valueIn}.
     * @throws InvalidMarshallableException If the throwable cannot be instantiated properly.
     */
    public static Throwable throwable(@NotNull ValueIn valueIn, boolean appendCurrentStack) throws InvalidMarshallableException {

        // Determine the type of the throwable from the valueIn
        @Nullable Class type = valueIn.typePrefix();

        // Create a new instance of the determined throwable type
        Throwable throwable = ObjectUtils.newInstance((Class<Throwable>) type);

        // Further process and return the throwable (the method for this is not provided)
        return throwable(valueIn, appendCurrentStack, throwable);
    }

    /**
     * Creates and processes a throwable object based on the data present in the provided {@code valueIn}.
     * The provided throwable is populated with a message and stack trace extracted from the {@code valueIn}.
     * Optionally, the current stack trace can be appended to the throwable's stack trace.
     *
     * @param valueIn           The value input containing the details of the throwable.
     * @param appendCurrentStack Flag indicating whether to append the current stack trace to the throwable.
     * @param throwable         The throwable to be populated with details from {@code valueIn}.
     * @return The processed throwable.
     * @throws InvalidMarshallableException If the data in {@code valueIn} cannot be processed properly.
     */
    protected static Throwable throwable(@NotNull ValueIn valueIn, boolean appendCurrentStack, Throwable throwable) throws InvalidMarshallableException {

        // Store the reference to the throwable being processed
        final Throwable finalThrowable = throwable;

        // List to hold the stack trace elements extracted from valueIn
        @NotNull final List<StackTraceElement> stes = new ArrayList<>();

        // Process the marshallable data in valueIn
        valueIn.marshallable(m -> {
            // Read and set the throwable's message
            @Nullable final String message = m.read(() -> "message").text();

            if (message != null) {
                try {
                    DETAILED_MESSAGE.set(finalThrowable, message);
                } catch (IllegalAccessException e) {
                    throw new AssertionError(e);
                }
            }

            // Extract and process the stack trace
            m.read(() -> "stackTrace").sequence(stes, (stes0, stackTrace) -> {
                while (stackTrace.hasNextSequenceItem()) {
                    stackTrace.marshallable(r -> {
                        // Extract details of each stack trace element
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

        // If appendCurrentStack is true, add current stack trace details
        if (appendCurrentStack) {
            stes.add(new StackTraceElement("~ remote", "tcp ~", "", 0));
            StackTraceElement[] stes2 = Thread.currentThread().getStackTrace();
            int first = 6;
            int last = Jvm.trimLast(first, stes2);
            // Loop to add each stack trace element from current thread's stack
            for (int i = first; i <= last; i++)
                stes.add(stes2[i]);
        }

        // Set the final stack trace to the throwable
        try {
            STACK_TRACE.set(finalThrowable, stes.toArray(NO_STE));
        } catch (IllegalAccessException e) {
            throw Jvm.rethrow(e);
        }
        return throwable;
    }

    /**
     * Merges two strings with a space in between. If one of the strings is null,
     * the other string is returned. If both are null, null is returned.
     *
     * @param a The first string.
     * @param b The second string.
     * @return The merged string or one of the strings if the other is null.
     */
    @Nullable
    static String merge(@Nullable String a, @Nullable String b) {
        return a == null ? b : b == null ? a : a + " " + b;
    }

    /**
     * Attempts to intern an object if it's of a type that is internable
     * and is actually a string. Otherwise, converts the object to the given class.
     *
     * @param tClass The class to which the object needs to be converted or interned.
     * @param o      The object to be interned or converted.
     * @param <T>    Type of the class.
     * @return The interned or converted object.
     */
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

    /**
     * @deprecated Use {@link Wires#acquireBytesScoped()} instead
     */
    @NotNull
    @Deprecated(/* To be removed in x.26 */)
    static Bytes<?> acquireInternalBytes() {
        if (Jvm.isDebug())
            return Bytes.allocateElasticOnHeap();
        Bytes<?> bytes = ThreadLocalHelper.getTL(INTERNAL_BYTES_TL,
                Wires::unmonitoredDirectBytes);
        bytes.clear();
        return bytes;
    }

    /**
     * @deprecated Use {@link Wires#acquireStringBuilderScoped()} instead
     */
    @Deprecated(/* To be removed in x.26 */)
    static StringBuilder acquireStringBuilderForValueIn() {
        return SBPVI.acquireStringBuilder();
    }

    /**
     * @deprecated Use {@link Wires#acquireStringBuilderScoped()} instead
     */
    @Deprecated(/* To be removed in x.26 */)
    static StringBuilder acquireStringBuilderForValueOut() {
        return SBPVO.acquireStringBuilder();
    }

    /**
     * A specialized class that interns objects based on their string representations.
     *
     * @param <T> Type of the object being interned.
     */
    static class ObjectInterner<T> extends FromStringInterner<T> {
        final Class<T> tClass;

        /**
         * Constructs an ObjectInterner for the specified class type with a
         * default capacity of 256.
         *
         * @param tClass The class type of the object to be interned.
         */
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
