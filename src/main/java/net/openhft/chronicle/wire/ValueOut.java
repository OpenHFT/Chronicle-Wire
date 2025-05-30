/*
 * Copyright 2016-2025 chronicle.software
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
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;
import net.openhft.chronicle.bytes.util.Compression;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.scoped.ScopedResource;
import net.openhft.chronicle.core.util.CoreDynamicEnum;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.core.values.*;
import net.openhft.chronicle.threads.NamedThreadFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static net.openhft.chronicle.wire.Wires.isScalar;

/**
 * After a field name is written by {@link WireOut#write(CharSequence)}, a
 * {@code ValueOut} serialises the value.  Implementations usually write a
 * single value and support many primitive and object types.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface ValueOut {

    /**
     * Thread-local {@link MapMarshaller} for efficient, thread-safe map serialisation.
     */
    ThreadLocal<MapMarshaller> MM_TL = ThreadLocal.withInitial(MapMarshaller::new);

    /**
     * Threshold in bytes below which {@link #compress(String, Bytes)} may skip compression.
     */
    int SMALL_MESSAGE = 64;

    /**
     * Utility string of sixty-four zeros, often used when commenting binary output.
     */
    String ZEROS_64 = "0000000000000000000000000000000000000000000000000000000000000000";

    /**
     * Utility to check whether {@code v} is a standard {@link Enum} or a {@link DynamicEnum}.
     */
    @SuppressWarnings("deprecation")
    static boolean isAnEnum(Object v) {
        return (v instanceof Enum) || (v instanceof DynamicEnum);
    }

    /**
     * Writes {@code flag} as this field's boolean value.
     *
     * @param flag value to write
     * @return parent wire for chaining
     */
    @NotNull
    WireOut bool(Boolean flag);

    /**
     * Writes the text value {@code s}.
     *
     * @param s text to write
     * @return parent wire for chaining
     */
    @NotNull
    WireOut text(@Nullable CharSequence s);

    /**
     * Convenience overload for {@link #text(CharSequence)}.
     */
    @NotNull
    default WireOut text(@Nullable String s) {
        return text((CharSequence) s);
    }

    /**
     * Writes a null representation for this field.
     *
     * @return parent wire for chaining
     */
    @NotNull
    default WireOut nu11() {
        return text((CharSequence) null);
    }

    /**
     * Writes the single character {@code c} as text.
     *
     * @param c character to write
     * @return parent wire for chaining
     */
    @NotNull
    default WireOut text(char c) {
        try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
            return text(stlSb.get().append(c));
        }
    }

    /**
     * Alias for {@link #text(char)}.
     */
    @NotNull
    default WireOut character(char c) {
        return text(c);
    }

    /**
     * Writes the content of {@code s} as text.
     *
     * @param s bytes to write
     * @return parent wire for chaining
     */
    @NotNull
    default WireOut text(@Nullable BytesStore<?, ?> s) {
        return text((CharSequence) s);
    }

    /**
     * Writes {@code x} as a signed 8‑bit value, checking the range.
     *
     * @param x value to write
     * @return parent wire for chaining
     * @throws ArithmeticException if {@code x} does not fit in a byte
     */
    @NotNull
    default WireOut int8(long x) {
        return int8(Maths.toInt8(x));
    }

    /**
     * Writes the byte {@code i8} as the current value.
     *
     * @param i8 byte to write
     * @return parent wire for chaining
     */
    @NotNull
    WireOut int8(byte i8);

    /**
     * Writes the content of {@code fromBytes} as bytes.
     *
     * @param fromBytes bytes to write
     * @return parent wire for chaining
     */
    @NotNull
    WireOut bytes(@Nullable BytesStore<?, ?> fromBytes);

    /**
     * Writes {@code fromBytes} as a literal byte sequence when the wire supports it.
     * Falls back to {@link #bytes(BytesStore)} otherwise.
     *
     * @param fromBytes bytes to write
     * @return parent wire for chaining
     */
    @NotNull
    default WireOut bytesLiteral(@Nullable BytesStore<?, ?> fromBytes) {
        return bytes(fromBytes);
    }

    /**
     * Writes a typed byte sequence using {@code type} as the type name.
     *
     * @param type      type identifier
     * @param fromBytes bytes to write
     * @return parent wire for chaining
     */
    @NotNull
    WireOut bytes(String type, @Nullable BytesStore<?, ?> fromBytes);

    /**
     * Writes {@code value} directly with minimal encoding.
     *
     * @param value bytes to write
     * @return parent wire for chaining
     */
    @NotNull
    WireOut rawBytes(byte[] value);

    /**
     * Writes {@code value} with minimal encoding, falling back to {@link #text(CharSequence)} if unsupported.
     *
     * @param value text to write
     * @return parent wire for chaining
     */
    @NotNull
    default WireOut rawText(CharSequence value) {
        return text(value);
    }

    /**
     * Writes a length prefix and returns this {@code ValueOut} to continue writing
     * the content.
     *
     * @param remaining number of bytes or elements to follow
     * @return this instance for chaining
     */
    @NotNull
    ValueOut writeLength(long remaining);

    /**
     * Writes the byte array {@code fromBytes}.
     *
     * @param fromBytes bytes to write
     * @return parent wire for chaining
     */
    @NotNull
    WireOut bytes(byte[] fromBytes);

    /**
     * Writes the byte array {@code fromBytes} with a type identifier.
     *
     * @param type      type identifier
     * @param fromBytes bytes to write
     * @return parent wire for chaining
     */
    @NotNull
    WireOut bytes(String type, byte[] fromBytes);

    /**
     * Writes {@code x} as an unsigned 8‑bit integer, checking the range.
     *
     * @param x value to write
     * @return parent wire for chaining
     * @throws ArithmeticException if {@code x} is out of range
     */
    @NotNull
    default WireOut uint8(int x) {
        return uint8checked(Maths.toUInt8(x & 0xFF));
    }

    /**
     * Writes {@code u8} as an unsigned 8‑bit integer without additional checks.
     */
    @NotNull
    WireOut uint8checked(int u8);

    /**
     * Writes {@code x} as a signed 16‑bit integer, checking the range.
     *
     * @param x value to write
     * @return parent wire for chaining
     * @throws ArithmeticException if {@code x} is out of range
     */
    @NotNull
    default WireOut int16(long x) {
        return int16(Maths.toInt16(x));
    }

    /**
     * Writes the short {@code i16} without extra range checking.
     */
    @NotNull
    WireOut int16(short i16);

    /**
     * Writes {@code x} as an unsigned 16‑bit integer.
     */
    @NotNull
    default WireOut uint16(long x) {
        return uint16checked((int) x);
    }

    /**
     * Writes {@code u16} as an unsigned 16‑bit integer without extra checks.
     */
    @NotNull
    WireOut uint16checked(int u16);

    /**
     * Writes a single Unicode code point as UTF-8.
     */
    @NotNull
    WireOut utf8(int codepoint);

    /**
     * Writes {@code x} as a signed 32‑bit integer, checking the range.
     *
     * @param x value to write
     * @return parent wire for chaining
     * @throws ArithmeticException if {@code x} is out of range
     */
    @NotNull
    default WireOut int32(long x) {
        return int32(Maths.toInt32(x));
    }

    /**
     * Writes the int {@code i32} without extra range checking.
     */
    @NotNull
    WireOut int32(int i32);

    /**
     * Writes {@code i32}, ignoring {@code previous} unless the wire supports delta encoding.
     */
    @NotNull
    default WireOut int32(int i32, int previous) {
        return int32(i32);
    }

    /**
     * Writes {@code x} as an unsigned 32‑bit integer.
     */
    @NotNull
    default WireOut uint32(long x) {
        return uint32checked(x);
    }

    /**
     * Writes {@code u32} as an unsigned 32‑bit integer without extra checks.
     */
    @NotNull
    WireOut uint32checked(long u32);

    /**
     * Writes {@code i64} as a signed 64‑bit integer.
     */
    @NotNull
    WireOut int64(long i64);

    /**
     * Writes {@code i64}, ignoring {@code previous} unless the wire supports delta encoding.
     */
    @NotNull
    default WireOut int64(long i64, long previous) {
        return int64(i64);
    }

    /**
     * Writes two longs and binds them to {@code value} for direct access.
     */
    @NotNull
    WireOut int128forBinding(long i64x0, long i64x1, TwoLongValue value);

    /**
     * Writes {@code i64} in hexadecimal form when the wire is textual.
     */
    @NotNull
    WireOut int64_0x(long i64);

    /**
     * Prepares space for a 64‑bit integer array of {@code capacity} elements.
     */
    @NotNull
    WireOut int64array(long capacity);

    /**
     * Writes an array of longs bound to {@code values} for direct access.
     */
    @NotNull
    WireOut int64array(long capacity, LongArrayValues values);

    /**
     * Writes {@code f} as a 32‑bit floating‑point number.
     */
    @NotNull
    WireOut float32(float f);

    /**
     * Writes {@code d} as a 64‑bit floating‑point number.
     */
    @NotNull
    WireOut float64(double d);

    /**
     * Writes {@code f}, ignoring {@code previous} unless the wire supports delta encoding.
     */
    @NotNull
    default WireOut float32(float f, float previous) {
        return float32(f);
    }

    /**
     * Writes {@code d}, ignoring {@code previous} unless the wire supports delta encoding.
     */
    @NotNull
    default WireOut float64(double d, double previous) {
        return float64(d);
    }

    /**
     * Writes the {@link LocalTime} value.
     */
    @NotNull
    WireOut time(LocalTime localTime);

    /**
     * Writes the {@link ZonedDateTime} value.
     */
    @NotNull
    WireOut zonedDateTime(ZonedDateTime zonedDateTime);

    /**
     * Writes the {@link LocalDate} value.
     */
    @NotNull
    WireOut date(LocalDate localDate);

    /**
     * Writes the {@link LocalDateTime} value.
     */
    @NotNull
    WireOut dateTime(LocalDateTime localDateTime);

    /**
     * Writes a type prefix such as {@code !com.acme.MyClass}.
     */
    @NotNull
    ValueOut typePrefix(CharSequence typeName);

    /**
     * Convenience overload for {@link #typePrefix(CharSequence)}.
     */
    @NotNull
    default ValueOut typePrefix(Class<?> type) {
        return type == null ? this : typePrefix(classLookup().nameFor(type));
    }

    /**
     * Provides a lookup mechanism to resolve class names. The exact mechanism and source
     * of class name data is determined by the implementation.
     *
     * @return A ClassLookup instance used for resolving class names.
     */
    ClassLookup classLookup();

    /**
     * Write a type literal for a specified {@link Class}. If the class is null, a null value
     * is written. This is used to denote type information directly in the wire format.
     *
     * @param type The Class object for which to write a type literal.
     * @return The WireOut instance for chained calls.
     */
    @NotNull
    default WireOut typeLiteral(@Nullable Class<?> type) {
        return type == null ? nu11()
                : typeLiteral((t, b) -> b.appendUtf8(classLookup().nameFor(t)), type);
    }

    /**
     * Write a type literal for a specified {@link Type}. If the type is null, a null value
     * is written. If the type is an instance of Class, then it delegates to the corresponding
     * method to write a type literal for a class.
     *
     * @param type The Type object for which to write a type literal.
     * @return The WireOut instance for chained calls.
     */
    @NotNull
    default WireOut typeLiteral(@Nullable Type type) {
        return type == null ? nu11()
                : type instanceof Class<?>? typeLiteral((Class) type)
                : typeLiteral(type.getTypeName());
    }

    /**
     * Write a type literal value. This is useful for wire formats that require direct type
     * annotations within the serialized data.
     *
     * @param type The type name as a CharSequence.
     * @return The WireOut instance for chained calls.
     */
    @NotNull
    WireOut typeLiteral(@Nullable CharSequence type);

    /**
     * Writes a type literal using the specified type translator function. This allows custom
     * translations for type literals depending on the implementation.
     *
     * @param typeTranslator A bi-consumer function to transform the Class object into a byte representation.
     * @param type The Class object to be translated.
     * @return The WireOut instance for chained calls.
     */
    @NotNull
    WireOut typeLiteral(@NotNull BiConsumer<Class, Bytes<?>> typeTranslator, @Nullable Class<?> type);

    /**
     * Writes a universally unique identifier (UUID) to the wire.
     *
     * @param uuid The UUID value to be written.
     * @return The WireOut instance for chained calls.
     */
    @NotNull
    WireOut uuid(UUID uuid);

    /**
     * Writes a 32-bit integer for binding. The specifics of "for binding" may be implementation dependent.
     */
    @NotNull
    WireOut int32forBinding(int value);

    /**
     * Writes a 32-bit integer for binding with the given IntValue. This may be used for additional metadata or configuration.
     */
    @NotNull
    WireOut int32forBinding(int value, @NotNull IntValue intValue);

    /**
     * Writes a 64-bit integer for binding. The specifics of "for binding" may be implementation dependent.
     */
    @NotNull
    WireOut int64forBinding(long value);

    /**
     * Throws an unsupported operation exception by default. May be overridden by specific implementations.
     */
    @NotNull
    default WireOut int128forBinding(long value, long value2) {
        throw new UnsupportedOperationException();
    }

    /**
     * Writes a 64-bit integer for binding with the given LongValue. This may be used for additional metadata or configuration.
     */
    @NotNull
    WireOut int64forBinding(long value, @NotNull LongValue longValue);

    /**
     * Writes a boolean value for binding with the given BooleanValue. This may be used for additional metadata or configuration.
     */
    @NotNull
    WireOut boolForBinding(boolean value, @NotNull BooleanValue longValue);

    /**
     * Writes a sequence of values to the wire using the provided writer. The default behavior uses the writer's `writeValue` method.
     */
    @NotNull
    default WireOut sequence(WriteValue writer) {
        return sequence(writer, WriteValue::writeValue);
    }

    /**
     * Writes a sequence of values from an {@link Iterator}. This method handles different types of iterables,
     * applying type prefixes as appropriate for Sets and SortedSets.
     *
     * @param t The Iterable of values to be written.
     * @return The WireOut instance for chained calls.
     */
    @NotNull
    default <T> WireOut sequence(Iterable<T> t) {
        final Class<?> typePrefix;
        if (t instanceof SortedSet)
            typePrefix = SortedSet.class;
        else if (t instanceof Set)
            typePrefix = Set.class;
        else
            typePrefix = null;

        if (typePrefix != null)
            typePrefix(typePrefix);

        final WireOut result = sequence(t, (it, out) -> {
            for (T o : it) {
                out.object(o);
            }
        });

        if (typePrefix != null)
            endTypePrefix();

        return result;
    }

    /**
     * Writes a sequence of values using the provided writer.
     *
     * @param t The input to be consumed by the writer.
     * @param writer A bi-consumer that writes values using the given ValueOut instance.
     * @return The WireOut instance for chained calls.
     */
    @NotNull <T> WireOut sequence(T t, BiConsumer<T, ValueOut> writer);

    /**
     * Writes a sequence of values using the provided parametrized writer.
     *
     * @param t The primary input to be consumed by the writer.
     * @param param A secondary input parameter for the writer.
     * @param writer A tri-consumer that writes values using the given ValueOut instance and additional parameter.
     * @return The WireOut instance for chained calls.
     * @throws InvalidMarshallableException When there's an issue marshalling the data.
     */
    @NotNull <T, K> WireOut sequence(T t, K param, TriConsumer<T, K, ValueOut> writer) throws InvalidMarshallableException;

    /**
     * Writes a sequence of values of a specified length.
     *
     * @param t The input to be consumed by the writer.
     * @param length The length of the sequence.
     * @param writer An object-int-object consumer that writes values using the given ValueOut instance.
     * @return The WireOut instance for chained calls.
     */
    default <T> WireOut sequenceWithLength(T t, int length, ObjectIntObjectConsumer<T, ValueOut> writer) {
        boolean b = swapLeaf(true);
        WireOut sequence = sequence(t, length, writer::accept);
        swapLeaf(b);
        return sequence;
    }

    /**
     * Writes an array of byte sequences of a specified length.
     *
     * @param array The array of byte sequences.
     * @param length The length of the sequence.
     * @return The WireOut instance for chained calls.
     */
    default WireOut array(Bytes[] array, int length) {
        return sequenceWithLength(array, length, (a, len, out) -> {
            for (int i = 0; i < len; i++)
                out.bytes(a[i]);
        });
    }

    /**
     * Writes an array of doubles of a specified length.
     *
     * @param array The array of doubles.
     * @param length The length of the sequence.
     * @return The WireOut instance for chained calls.
     */
    default WireOut array(double[] array, int length) {
        return sequenceWithLength(array, length, (a, len, out) -> {
            for (int i = 0; i < len; i++)
                out.float64(a[i]);
        });
    }

    /**
     * This write value's relative to the first one using 6 digit precision
     *
     * @param array  to write
     * @param length to write
     * @return this
     */
    default WireOut arrayDelta(double[] array, int length) {
        return sequenceWithLength(array, length, (a, len, out) -> {
            if (len <= 0) return;
            out.float64(a[0]);
            double a0 = a[0];
            if (Double.isNaN(a0)) a0 = 0.0;
            for (int i = 1; i < len; i++)
                out.float64(Maths.round6(a[i] - a0));
        });
    }

    /**
     * Writes an array of boolean values to the wire output.
     *
     * @param array  The array of booleans to be written.
     * @param length The number of elements from the array to write.
     * @return The current instance of the WireOut.
     */
    default WireOut array(boolean[] array, int length) {
        return sequenceWithLength(array, length, (a, len, out) -> {
            for (int i = 0; i < len; i++)
                out.bool(a[i]);
        });
    }

    /**
     * Writes an array of long values to the wire output.
     *
     * @param array  The array of longs to be written.
     * @param length The number of elements from the array to write.
     * @return The current instance of the WireOut.
     */
    default WireOut array(long[] array, int length) {
        return sequenceWithLength(array, length, (a, len, out) -> {
            for (int i = 0; i < len; i++)
                out.int64(a[i]);
        });
    }

    /**
     * Writes the delta of long values in an array to the wire output.
     * The first value is written as is, and the subsequent values are written as differences
     * from the first value.
     *
     * @param array  The array of longs whose deltas are to be written.
     * @param length The number of elements from the array to write.
     * @return The current instance of the WireOut.
     */
    default WireOut arrayDelta(long[] array, int length) {
        return sequenceWithLength(array, length, (a, len, out) -> {
            if (len <= 0) return;
            out.int64(a[0]);
            long a0 = a[0];
            for (int i = 1; i < len; i++)
                out.int64(a[i] - a0);
        });
    }

    /**
     * Writes an array of int values to the wire output.
     *
     * @param array  The array of ints to be written.
     * @param length The number of elements from the array to write.
     * @return The current instance of the WireOut.
     */
    default WireOut array(int[] array, int length) {
        return sequenceWithLength(array, length, (a, len, out) -> {
            for (int i = 0; i < len; i++)
                out.int32(a[i]);
        });
    }

    /**
     * Writes an array of byte values to the wire output.
     *
     * @param array  The array of bytes to be written.
     * @param length The number of elements from the array to write.
     * @return The current instance of the WireOut.
     */
    default WireOut array(byte[] array, int length) {
        return sequenceWithLength(array, length, (a, len, out) -> {
            for (int i = 0; i < len; i++)
                out.int8(a[i]);
        });
    }

    /**
     * Writes an array of specified type objects to the wire output, using the provided writer.
     * Recognizes arrays of type String[] and other object arrays, appending appropriate type prefixes.
     *
     * @param writer     The writer to use for writing the array.
     * @param arrayType  The type of the array to be written.
     * @return The current instance of the WireOut.
     */
    @NotNull
    default WireOut array(@NotNull WriteValue writer, @NotNull Class<?> arrayType) {
        if (arrayType == String[].class) {
            typePrefix("String[] ");
        } else if (arrayType != Object[].class) {
            typePrefix(classLookup().nameFor(arrayType.getComponentType()) + "[]");
        }
        return sequence(writer);
    }

    /**
     * Writes a value that implements the {@link WriteMarshallable} interface to the wire output.
     *
     * @param object The WriteMarshallable object to be written.
     * @return The current instance of the WireOut.
     * @throws InvalidMarshallableException If the object cannot be marshalled.
     */
    @NotNull
    WireOut marshallable(WriteMarshallable object) throws InvalidMarshallableException;

    /**
     * Writes a value that implements the {@link Serializable} interface to the wire output.
     *
     * @param object The Serializable object to be written.
     * @return The current instance of the WireOut.
     * @throws InvalidMarshallableException If the object cannot be marshalled.
     */
    @NotNull
    WireOut marshallable(Serializable object) throws InvalidMarshallableException;

    /**
     * Writes the contents of a specified map to the wire output.
     * The key and value type of the map must be either Marshallable, String, or Autoboxed primitives.
     *
     * @param map The map whose contents are to be written.
     * @return The current instance of the WireOut.
     * @throws InvalidMarshallableException If the map cannot be marshalled.
     * @throws IllegalArgumentException If the type of the map is not one of those listed above.
     */
    @NotNull
    WireOut map(Map map) throws InvalidMarshallableException;

    /**
     * Determines if the current WireOut is in a leaf node state. This method is intended for internal use.
     * The default behavior is to always return false.
     *
     * @param isLeaf Flag indicating whether the current node is a leaf.
     * @return A boolean indicating the previous state (always false in the default implementation).
     */
    default boolean swapLeaf(boolean isLeaf) {
        return false;
    }

    /**
     * To be used when you know it is a typed marshallable object. e.g. an Enum is not, it's a scalar.
     * If you are not sure, use the {@link #object(Object)} method.
     *
     * @param marshallable to write
     * @return the original wire
     */
    @NotNull
    default WireOut typedMarshallable(@Nullable WriteMarshallable marshallable) throws InvalidMarshallableException {
        if (marshallable == null)
            return nu11();
        String typeName = Wires.typeNameFor(classLookup(), marshallable);
        if (typeName != null)
            typePrefix(typeName);
        final WireOut wire = marshallable(marshallable);
        if (typeName != null)
            endTypePrefix();
        return wire;
    }

    /**
     * Ends the type prefix scope for the written data.
     * In the default implementation, this method does nothing. Override it in subclasses if specific end logic is required.
     */
    default void endTypePrefix() {

    }

    /**
     * Writes a value that implements the {@link Serializable} interface to the wire output.
     * Specifically used when the object is known to be a typed serializable. If uncertain about the object's type,
     * it's recommended to use the {@link #object(Object)} method instead.
     *
     * @param object The Serializable object to be written.
     * @return The current instance of the WireOut.
     * @throws InvalidMarshallableException If the object cannot be marshalled.
     */
    @NotNull
    default WireOut typedMarshallable(@Nullable Serializable object) throws InvalidMarshallableException {
        if (object == null)
            return nu11();

        try {
            typePrefix(object.getClass());
            if (object instanceof WriteMarshallable) {
                return marshallable((WriteMarshallable) object);
            } else if (object instanceof Enum) {
                return asEnum((Enum) object);
            } else if (isScalar(object)) {
                if (object instanceof LocalDate) {
                    LocalDate d = (LocalDate) object;
                    try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                        return text(stlSb.get()
                                .append(d.getYear())
                                .append('-')
                                .append(d.getMonthValue() < 10 ? "0" : "")
                                .append(d.getMonthValue())
                                .append('-')
                                .append(d.getDayOfMonth() < 10 ? "0" : "")
                                .append(d.getDayOfMonth()));
                    }
                }
                return text(object.toString());
            } else if (object instanceof Locale) {
                return text(((Locale) object).toLanguageTag());
            } else {
                return marshallable(object);
            }
        } finally {
            // Ensure the type prefix scope is closed, even if there was an error
            endTypePrefix();
        }
    }

    /**
     * Writes a value that implements the {@link WriteMarshallable} interface to the wire output,
     * while prepending it with a specified type prefix.
     *
     * @param typeName The type prefix to be written before the object.
     * @param object   The WriteMarshallable object to be written.
     * @return The current instance of the WireOut.
     * @throws InvalidMarshallableException If the object cannot be marshalled.
     */
    @NotNull
    default WireOut typedMarshallable(CharSequence typeName, WriteMarshallable object) throws InvalidMarshallableException {
        typePrefix(typeName);
        return marshallable(object);
    }

    /**
     * Writes an enum value to the wire output.
     * The enum's name is used as its string representation.
     *
     * @param <E> Type parameter indicating that the provided object is of an Enum type.
     * @param e   The enum value to be written.
     * @return The current instance of the WireOut.
     */
    @NotNull
    default <E extends Enum<E>> WireOut asEnum(@Nullable E e) {
        return text(e == null ? null : e.name());
    }

    /**
     * Writes a set value to the wire output. The specific type of entries is not specified.
     *
     * @param <V>  The type of elements in the set.
     * @param coll The set to be written.
     * @return The current instance of the WireOut.
     * @throws InvalidMarshallableException If the set cannot be marshalled.
     */
    @NotNull
    default <V> WireOut set(Set<V> coll) throws InvalidMarshallableException {
        return set(coll, null);
    }

    /**
     * Writes a set to the wire output, while specifying the type of its entries.
     *
     * @param <V>          The type of elements in the set.
     * @param coll         The set to be written.
     * @param assumedClass The expected class type of the set entries.
     * @return The current instance of the WireOut.
     * @throws InvalidMarshallableException If the set cannot be marshalled.
     */
    @NotNull
    default <V> WireOut set(Set<V> coll, Class<V> assumedClass) throws InvalidMarshallableException {
        return collection(coll, assumedClass);
    }

    /**
     * Writes a list to the wire output. The specific type of list entries is not specified.
     *
     * @param <V>  The type of elements in the list.
     * @param coll The list to be written.
     * @return The current instance of the WireOut.
     * @throws InvalidMarshallableException If the list cannot be marshalled.
     */
    @NotNull
    default <V> WireOut list(List<V> coll) throws InvalidMarshallableException {
        return list(coll, null);
    }

    /**
     * Writes a list to the wire output, while specifying the type of its entries.
     *
     * @param <V>          The type of elements in the list.
     * @param coll         The list to be written.
     * @param assumedClass The expected class type of the list entries.
     * @return The current instance of the WireOut.
     * @throws InvalidMarshallableException If the list cannot be marshalled.
     */
    @NotNull
    default <V> WireOut list(List<V> coll, Class<V> assumedClass) throws InvalidMarshallableException {
        // Write the list to the output
        sequence(coll, assumedClass, (s, kls, out) -> {
            int size = s.size();
            // Iterate through the list and marshall each item
            for (int i = 0; i < size; i++) {
                boolean wasLeaf = out.swapLeaf(true);
                marshallable((WriteMarshallable) s.get(i));
                out.swapLeaf(wasLeaf);
            }
        });
        return wireOut();
    }

    /**
     * Writes a collection to the wire output, specifying the type of its entries.
     *
     * @param <V>          The type of elements in the collection.
     * @param coll         The collection to be written.
     * @param assumedClass The expected class type of the collection entries.
     * @return The current instance of the WireOut.
     * @throws InvalidMarshallableException If the collection cannot be marshalled.
     */
    @NotNull
    default <V> WireOut collection(Collection<V> coll, Class<V> assumedClass) throws InvalidMarshallableException {
        // Write the collection to the output
        sequence(coll, assumedClass, (s, kls, out) -> {
            // Iterate through the collection and write each item
            for (V v : s) {
                object(kls, v);
            }
        });
        return wireOut();
    }

    /**
     * Writes an object to the wire output, specifying its expected type.
     *
     * @param <V>           Type of the object to be written.
     * @param expectedType  The expected class type of the object.
     * @param v             The object to be written.
     * @return The current instance of the WireOut.
     * @throws InvalidMarshallableException If the object cannot be marshalled.
     */
    @NotNull
    default <V> WireOut object(@NotNull Class<? extends V> expectedType, V v) throws InvalidMarshallableException {
        Class<?> vClass = v == null ? void.class : v.getClass();
        // Check for various types and marshall accordingly
        if (v instanceof WriteMarshallable && !isAnEnum(v))
            if (ObjectUtils.matchingClass(expectedType, vClass)) {
                marshallable((WriteMarshallable) v);
            } else {
                typedMarshallable((WriteMarshallable) v);
            }
        else if (v != null && ObjectUtils.matchingClass(expectedType, vClass))
            untypedObject(v);
        else
            object(v);
        return wireOut();
    }

    /**
     * Writes a map to the wire output. Uses default object classes for keys and values.
     *
     * @param <K> Type of the map key.
     * @param <V> Type of the map value.
     * @param map The map to be written.
     * @return The current instance of the WireOut.
     * @throws InvalidMarshallableException If the map cannot be marshalled.
     */
    @NotNull
    default <K, V> WireOut marshallable(Map<K, V> map) throws InvalidMarshallableException {
        return marshallable(map, (Class) Object.class, (Class) Object.class, true);
    }

    /**
     * Writes a map to the wire output, specifying classes for keys and values.
     *
     * @param <K>     Type of the map key.
     * @param <V>     Type of the map value.
     * @param map     The map to be written. Can be null.
     * @param kClass  Class type of the map key.
     * @param vClass  Class type of the map value.
     * @param leaf    Indicates if the map is a leaf node.
     * @return The current instance of the WireOut.
     * @throws InvalidMarshallableException If the map cannot be marshalled.
     */
    @NotNull
    default <K, V> WireOut marshallable(@Nullable Map<K, V> map, @NotNull Class<K> kClass, @NotNull Class<V> vClass, boolean leaf) throws InvalidMarshallableException {
        if (map == null) {
            nu11();
            return wireOut();
        }

        final MapMarshaller mapMarshaller = MM_TL.get();
        mapMarshaller.params(map, kClass, vClass, leaf);
        marshallable(mapMarshaller);
        return wireOut();
    }

    /**
     * Writes an object to the wire output. This method is optimized to handle various known types
     * and provides custom serialization for each.
     *
     * @param value The object to be written. Can be null.
     * @return The current instance of the WireOut.
     * @throws InvalidMarshallableException If the object cannot be marshalled.
     */
    @NotNull
    default WireOut object(@Nullable Object value) throws InvalidMarshallableException {
        if (value == null)
            return nu11();
        // Look for exact class matches for optimized serialization
        final Class<?> valueClass = value.getClass();
        switch (valueClass.getName()) {
            case "[B": { // Byte array
                typePrefix(byte[].class).bytes((byte[]) value);
                endTypePrefix();
                return wireOut();
            }
            case "[S":
            case "[C":
            case "[I":
            case "[J":
            case "[F":
            case "[D":
            case "[Z":
                ValueOut valueOut = typePrefix(valueClass);
                boolean wasLeaf = valueOut.swapLeaf(true);
                valueOut.sequence(value, (v, out) -> {
                    int len = Array.getLength(v);
                    for (int i = 0; i < len; i++) {
                        out.untypedObject(Array.get(v, i));
                    }
                });
                valueOut.swapLeaf(wasLeaf);
                endTypePrefix();
                return wireOut();

            case "net.openhft.chronicle.threads.NamedThreadFactory":
                return text(((NamedThreadFactory) value).getName());
            case "net.openhft.chronicle.wire.RawText":
                return rawText(((RawText) value).text);
            case "java.util.concurrent.atomic.AtomicReference":
                return object(((AtomicReference) value).get());
            case "java.util.concurrent.atomic.AtomicLong.class":
                return int64(((AtomicLong) value).get());
            case "java.util.concurrent.atomic.AtomicInteger":
                return int32(((AtomicInteger) value).get());
            case "java.util.concurrent.atomic.AtomicBoolean":
                return bool(((AtomicBoolean) value).get());
            case "java.lang.String":
                return text((String) value);
            case "java.lang.StringBuilder":
                return text((StringBuilder) value);
            case "java.lang.Byte":
                return fixedInt8((byte) value);
            case "java.lang.Boolean":
                return bool((Boolean) value);
            case "java.lang.Character":
                return text(value.toString());
            case "java.lang.Class":
                return typeLiteral((Class) value);
            case "java.lang.Short":
                return fixedInt16((short) value);
            case "java.lang.Integer":
                return fixedInt32((int) value);
            case "java.lang.Long":
                return fixedInt64((long) value);
            case "java.lang.Double":
                return fixedFloat64((double) value);
            case "java.lang.Float":
                return fixedFloat32((float) value);
            case "[Ljava.lang.String;":
                return array(v -> Stream.of((String[]) value).forEach(v::text), Object[].class);
            case "[Ljava.lang.Object;":
                return array(v -> Stream.of((Object[]) value).forEach(v::object), Object[].class);
            case "java.time.LocalTime": {
                final WireOut result = optionalTyped(LocalTime.class).time((LocalTime) value);
                endTypePrefix();
                return result;
            }
            case "java.time.LocalDate": {
                final WireOut result = optionalTyped(LocalDate.class).date((LocalDate) value);
                endTypePrefix();
                return result;
            }
            case "java.time.LocalDateTime": {
                final WireOut result = optionalTyped(LocalDateTime.class).dateTime((LocalDateTime) value);
                endTypePrefix();
                return result;
            }
            case "java.time.ZonedDateTime": {
                final WireOut result = optionalTyped(ZonedDateTime.class).zonedDateTime((ZonedDateTime) value);
                endTypePrefix();
                return result;
            }
            case "java.util.BitSet": {
                typePrefix(BitSet.class);

                BitSet bs = (BitSet) value;
                boolean isYamlWire = YamlWireOut.YamlValueOut.class.isAssignableFrom(this.getClass());

                // note : this like the others below this is capturing lambda
                return sequence(v -> {
                    for (int i = 0; i < bs.size() >> 6; i++) {
                        long l = BitSetUtil.getWord(bs, i);
                        WireOut wireOut = v.int64(l);
                        if (isYamlWire) {
                            String bits = Long.toBinaryString(l);
                            wireOut.writeComment(ZEROS_64.substring(bits.length()) + bits);
                        }
                    }
                });
            }

            case "java.util.UUID": {
                final WireOut result = optionalTyped(UUID.class).uuid((UUID) value);
                endTypePrefix();
                return result;
            }

            case "java.sql.Timestamp":
            case "java.sql.Time":
            case "java.util.Date":
            case "java.sql.Date": {
                final WireOut result = Wires.SerializeJavaLang.writeDate((Date) value, typePrefix(valueClass));
                endTypePrefix();
                return result;
            }

            case "java.util.GregorianCalendar": {
                GregorianCalendar gc = (GregorianCalendar) value;
                final WireOut result = typePrefix(GregorianCalendar.class).untypedObject(gc.toZonedDateTime());
                endTypePrefix();
                return result;
            }

            case "java.math.BigInteger":
            case "java.math.BigDecimal":
            case "java.time.Duration":
            case "java.io.File": {
                final WireOut result = optionalTyped(valueClass).text(value.toString());
                endTypePrefix();
                return result;
            }
        }
        // Check if the value is an instance of WriteMarshallable interface
        if (value instanceof WriteMarshallable) {
            // Handle the case where the value is an enum type
            if (isAnEnum(value)) {
                Jvm.debug().on(getClass(), "Treating " + valueClass + " as enum not WriteMarshallable");
                return typedScalar(value);
            }
            return Jvm.isLambdaClass(valueClass)
                    ? marshallable((WriteMarshallable) value)
                    : typedMarshallable((WriteMarshallable) value);
        }

        // Check if the value is an instance of WriteBytesMarshallable interface
        if (value instanceof WriteBytesMarshallable) {
            // Warn about possible unmarshalling issue
            if (!Wires.warnedUntypedBytesOnce) {
                Jvm.warn().on(ValueOut.class, "BytesMarshallable found in field which is not matching exactly, " +
                        "the object may not unmarshall correctly if that type is not specified: " + valueClass.getName() +
                        ". The warning will not repeat so there may be more types affected.");

                Wires.warnedUntypedBytesOnce = true;
            }

            return bytesMarshallable((BytesMarshallable) value);
        }

        // Handle other known types
        if (value instanceof BytesStore)
            return bytes((BytesStore) value);
        if (value instanceof CharSequence)
            return text((CharSequence) value);
        if (value instanceof Map) {
            if (value instanceof SortedMap)
                typePrefix(SortedMap.class);

            return map((Map) value);
        }
        if (value instanceof Throwable)
            return throwable((Throwable) value);
        if (isAnEnum(value))
            return typedScalar(value);
        if (value instanceof Collection) {
            if (value instanceof SortedSet)
                typePrefix(SortedSet.class);
            else if (value instanceof Set)
                typePrefix(Set.class);
            WireOut wireOut = sequence(v -> ((Collection) value).forEach(v::object));
            if (value instanceof Set)
                endTypePrefix();
            return wireOut;
        } else if (WireSerializedLambda.isSerializableLambda(valueClass)) {
            WireSerializedLambda.write(value, this);
            return wireOut();
        } else if (Object[].class.isAssignableFrom(valueClass)) {
            @NotNull Class<?> type = valueClass.getComponentType();
            return array(v -> Stream.of((Object[]) value).forEach(val -> v.object((Class) type, val)), valueClass);
        } else if (value instanceof Thread) {
            return text(((Thread) value).getName());
        } else if (value instanceof Serializable) {
            return typedMarshallable((Serializable) value);
        } else if (value instanceof ByteBuffer) {
            return object(BytesStore.wrap((ByteBuffer) value));
        } else if (value instanceof LongValue) {
            LongValue value2 = (LongValue) value;
            return int64forBinding(value2.getValue(), value2);
        } else if (value instanceof IntValue) {
            IntValue value2 = (IntValue) value;
            return int32forBinding(value2.getValue(), value2);
        } else if (value instanceof Reference) {
            return object(((Reference) value).get());
        } else {
            if ((Wires.isInternal(value)))
                throw new IllegalArgumentException("type=" + valueClass +
                        " is unsupported, it must either be of type Marshallable, String or " +
                        "AutoBoxed primitive Object");
            // Try to determine the type name
            String typeName;
            try {
                typeName = Wires.typeNameFor(classLookup(), value);
            } catch (IllegalArgumentException e) {
                if (isBinary())
                    throw e;
                typeName = valueClass.getName();
            }
            // Add type prefix if typeName is known
            if (typeName != null)
                typePrefix(typeName);
            // Default marshalling
            marshallable(w -> Wires.writeMarshallable(value, w));
            if (typeName != null)
                endTypePrefix();
            return wireOut();
        }
    }

    /**
     * Serialize an object implementing the WriteBytesMarshallable interface.
     * However, this is unsupported in the default implementation and will throw an exception.
     */
    default WireOut bytesMarshallable(WriteBytesMarshallable value) throws InvalidMarshallableException {
        throw new UnsupportedOperationException();
    }

    /**
     * Add an optional type i.e. if TEXT is used.
     * In the default implementation, this method doesn't make any changes and returns 'this'.
     *
     * @param aClass to write
     * @return this
     */
    @NotNull
    default ValueOut optionalTyped(Class<?> aClass) {
        return this;
    }

    /**
     * Serialize a float value prefixed with its type.
     */
    @NotNull
    default WireOut fixedFloat32(float value) {
        return typePrefix(float.class).float32(value);
    }

    /**
     * Serialize a byte (signed 8-bit) value prefixed with its type.
     */
    @NotNull
    default WireOut fixedInt8(byte value) {
        return typePrefix(byte.class).int8(value);
    }

    /**
     * Serialize a short (signed 16-bit) value prefixed with its type.
     */
    @NotNull
    default WireOut fixedInt16(short value) {
        return typePrefix(short.class).int16(value);
    }

    /**
     * Serialize an int (signed 32-bit) value prefixed with its type.
     */
    @NotNull
    default WireOut fixedInt32(int value) {
        return typePrefix(int.class).int32(value);
    }

    /**
     * Serialize a double value.
     * Notice that there's no type prefixing in this default implementation.
     */
    @NotNull
    default WireOut fixedFloat64(double value) {
        return float64(value);
    }

    /**
     * Serialize a long (signed 64-bit) value.
     * Again, there's no type prefixing in this default implementation.
     */
    @NotNull
    default WireOut fixedInt64(long value) {
        return int64(value);
    }

    /**
     * Write an untyped object value.
     * This method attempts to serialize an object without the caller explicitly specifying the object type.
     * This is done by checking the object's class name against a list of known class names and using appropriate serialization methods.
     */
    @SuppressWarnings("deprecation")
    @NotNull
    default WireOut untypedObject(@Nullable Object value) throws InvalidMarshallableException {
        // Handle null value case first
        if (value == null)
            return nu11();
        // Switch on the class name of the object for known types
        switch (value.getClass().getName()) {
            // Directly serialize byte arrays
            case "[B":
                return bytes((byte[]) value);
            // Handle primitive wrapper types
            case "java.lang.Byte":
                return int8((Byte) value);
            case "java.lang.Short":
                return int16((Short) value);
            case "java.lang.Integer":
                return int32((Integer) value);
            case "java.lang.Long":
                return int64((Long) value);
            case "java.lang.Double":
                return float64((Double) value);
            case "java.lang.Float":
                return float32((Float) value);
            // Handle date and time related classes from Java 8 date-time API
            case "java.time.LocalTime":
                return time((LocalTime) value);
            case "java.time.LocalDate":
                return date((LocalDate) value);
            case "java.time.LocalDateTime":
                return dateTime((LocalDateTime) value);
            case "java.time.ZonedDateTime":
                return zonedDateTime((ZonedDateTime) value);
            case "java.util.UUID":
                return uuid((UUID) value);
            // Serialize objects that can be represented as text
            case "java.math.BigInteger":
            case "java.math.BigDecimal":
            case "java.io.File":
                return text(value.toString());
        }
        // Check if value is an Enum and get its name
        if (isAnEnum(value)) {
            String name = value instanceof DynamicEnum
                    ? ((DynamicEnum) value).name()
                    : ((Enum) value).name();
            return text(name);
        }
        // Check if value implements Marshallable interface and serialize accordingly
        if (value instanceof Marshallable)
            return marshallable((Marshallable) value);
        // Check if value implements WriteBytesMarshallable interface and serialize
        if (value instanceof WriteBytesMarshallable)
            return bytesMarshallable((BytesMarshallable) value);
        // Handle object arrays
        if (Object[].class.isAssignableFrom(value.getClass())) {
            @NotNull Class<?> type = value.getClass().getComponentType();
            return array(v -> Stream.of((Object[]) value).forEach(val -> v.object((Class) type, val)), Object[].class);
        }
        // Default serialization for other types
        return object(value);
    }

    /**
     * Write a typed scalar value as type prefixed text.
     * This method ensures the serialized value is prefixed with its type to aid deserialization.
     */
    @NotNull
    default WireOut typedScalar(@NotNull Object value) {
        // Prefix with the type of the value
        typePrefix(Wires.typeNameFor(classLookup(), value));

        // Check if value is an Enum and get its name
        if (value instanceof Enum)
            value = ((Enum) value).name();
        if (value instanceof CoreDynamicEnum)
            value = ((CoreDynamicEnum) value).name();
        // Convert other objects to their string representation
        else if (!(value instanceof CharSequence))
            value = value.toString();

        // Serialize the value as text
        text((CharSequence) value);
        endTypePrefix();
        return wireOut();
    }

    /**
     * Write a throwable value.
     * This method serializes a Throwable object into the wire format. It captures the message, stack trace,
     * and any nested cause of the Throwable.
     */
    @NotNull
    default WireOut throwable(@NotNull Throwable t) throws InvalidMarshallableException {
        // Start by writing the fully qualified name of the throwable class
        typedMarshallable(t.getClass().getName(), (WireOut w) -> {
            // Write the message of the throwable
            w.write("message").text(t.getMessage())
                    // Start a sequence for the stack trace
                    .write("stackTrace").sequence(w3 -> {
                        for (StackTraceElement ste : t.getStackTrace()) {
                            // For each stack trace element, serialize its properties
                            w3.marshallable(w4 ->
                                    w4.write("class").text(ste.getClassName())
                                            .write("method").text(ste.getMethodName())
                                            .write("file").text(ste.getFileName())
                                            .write("line").int32(ste.getLineNumber()));
                        }
                    });
            // If there's a cause for this throwable, serialize it recursively
            if (t.getCause() != null)
                w.write("cause").throwable(t.getCause());
        });
        // Finish the serialization and return the WireOut object
        return wireOut();
    }

    // This method seems to provide an interface to the underlying wire format object
    @NotNull
    WireOut wireOut();

    /**
     * Compresses the given bytes using the specified compression technique.
     * If the byte size is below a certain threshold (SMALL_MESSAGE), the bytes are written uncompressed.
     */
    @NotNull
    default WireOut compress(@NotNull String compression, @Nullable Bytes<?> uncompressedBytes) {
        // Handle the case of null bytes
        if (uncompressedBytes == null)
            return nu11();
        // If the byte size is smaller than the threshold, just write the bytes directly
        if (uncompressedBytes.readRemaining() < SMALL_MESSAGE)
            return bytes(uncompressedBytes);
        try (ScopedResource<Bytes<Void>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> tmpBytes = stlBytes.get();
            Compression.compress(compression, uncompressedBytes, tmpBytes);
        // Write the compressed bytes
            bytes(compression, tmpBytes);
            return wireOut();
        }
    }

    // Gets the size of the compressed data, if available, or returns max int value as a default
    default int compressedSize() {
        return Integer.MAX_VALUE;
    }

    // Resets the state of the wire to allow for handling multiple documents in the wire format
    default void resetBetweenDocuments() {
        resetState();
    }

    // Resets the state of the wire, preparing it for the next operation or data write
    void resetState();

    /**
     * Indicates if the current wire type is a binary wire.
     * This could be useful for differentiating between textual or binary wire formats.
     *
     * @return {@code true} if this wire type is binary wire, {@code false} otherwise.
     */
    default boolean isBinary() {
        return false;
    }

    /**
     * Writes a boolean value to the wire.
     * Internally, this method leverages the bool method to perform the actual writing.
     *
     * @param x The boolean value to be written.
     * @return The WireOut instance after the operation.
     */
    default WireOut writeBoolean(boolean x) {
        return bool(x);
    }

    /**
     * Writes a byte value to the wire.
     * Utilizes the int8 method to write the byte.
     *
     * @param x The byte value to be written.
     * @return The WireOut instance after the operation.
     */
    default WireOut writeByte(byte x) {
        return int8(x);
    }

    /**
     * Writes a char value to the wire.
     * Leverages the uint16 method, characters are stored as unsigned 16-bit integers.
     *
     * @param x The char value to be written.
     * @return The WireOut instance after the operation.
     */
    default WireOut writeChar(char x) {
        return uint16(x);
    }

    /**
     * Writes a short value to the wire.
     * Uses the int16 method for the writing operation.
     *
     * @param x The short value to be written.
     * @return The WireOut instance after the operation.
     */
    default WireOut writeShort(short x) {
        return int16(x);
    }

    /**
     * Writes an int value to the wire.
     * Relies on the int32 method to carry out the write.
     *
     * @param x The integer value to be written.
     * @return The WireOut instance after the operation.
     */
    default WireOut writeInt(int x) {
        return int32(x);
    }

    /**
     * Writes a long value to the wire.
     * Uses the int64 method for the operation.
     *
     * @param x The long value to be written.
     * @return The WireOut instance after the operation.
     */
    default WireOut writeLong(long x) {
        return int64(x);
    }

    /**
     * Writes a float value to the wire.
     * Executes the write using the float32 method.
     *
     * @param x The float value to be written.
     * @return The WireOut instance after the operation.
     */
    default WireOut writeFloat(float x) {
        return float32(x);
    }

    /**
     * Writes a double value to the wire.
     * Uses the float64 method to carry out the writing operation.
     *
     * @param x The double value to be written.
     * @return The WireOut instance after the operation.
     */
    default WireOut writeDouble(double x) {
        return float64(x);
    }

    /**
     * Writes a string or a sequence of characters to the wire.
     * Uses the text method for the writing process.
     *
     * @param x The sequence of characters to be written.
     * @return The WireOut instance after the operation.
     */
    default WireOut writeString(CharSequence x) {
        return text(x);
    }

    /**
     * Writes an int value to the wire using a specified converter.
     * The converter helps in changing the format or representation of the integer.
     *
     * @param converter Converter to use.
     * @param i The integer to be converted and written.
     * @return The WireOut instance after the operation.
     */
    default WireOut writeInt(LongConverter converter, int i) {
        try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
            StringBuilder sb = stlSb.get();
            converter.append(sb, i);
            return rawText(sb);
        }
    }

    /**
     * Writes a long value to the wire using a specified converter.
     * The converter helps in changing the format or representation of the long value.
     *
     * @param longConverter Converter to use.
     * @param l The long value to be converted and written.
     * @return The WireOut instance after the operation.
     */
    default WireOut writeLong(LongConverter longConverter, long l) {
        try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
            StringBuilder sb = stlSb.get();
            longConverter.append(sb, l);
            if (longConverter.allSafeChars() && sb.length() > 0)
                return rawText(sb);
            else
                return text(sb);
        }
    }

    /**
     * Signifies the end of an element that has been added direcftly to the wire.
     */
    default void elementSeparator() {
        // Do nothing in the default implementation
    }

    /**
     * MapMarshaller is a utility for serializing a Map into a Wire format.
     * This is an inner class used for handling the custom marshalling process for Map objects.
     * Its primary function is to loop through a Map's entries and write each key-value pair to the Wire.
     */
    class MapMarshaller<K, V> implements WriteMarshallable {
        private Map<K, V> map;
        private Class<K> kClass;
        private Class<V> vClass;
        private boolean leaf;

        /**
         * Configures the MapMarshaller with the provided parameters.
         *
         * @param map The map to be marshalled.
         * @param kClass The class type of the map's key.
         * @param vClass The class type of the map's value.
         * @param leaf A flag indicating if the current node is a leaf in a structure.
         */
        void params(@Nullable Map<K, V> map, @NotNull Class<K> kClass, @NotNull Class<V> vClass, boolean leaf) {
            this.map = map;
            this.kClass = kClass;
            this.vClass = vClass;
            this.leaf = leaf;
        }

        /**
         * Converts and writes the Map's entries to the Wire format.
         *
         * @param wire The WireOut instance to write to.
         */
        @Override
        public void writeMarshallable(@NotNull WireOut wire) throws InvalidMarshallableException {
            for (@NotNull Map.Entry<K, V> entry : map.entrySet()) {
                ValueOut valueOut = wire.writeEvent(kClass, entry.getKey());
                boolean wasLeaf = valueOut.swapLeaf(leaf);
                valueOut.object(vClass, entry.getValue());
                valueOut.swapLeaf(wasLeaf);
            }
        }
    }
}
