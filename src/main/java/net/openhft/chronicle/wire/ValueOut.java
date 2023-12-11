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
 * Write out data after writing a field.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface ValueOut {
    ThreadLocal<MapMarshaller> MM_TL = ThreadLocal.withInitial(MapMarshaller::new);

    int SMALL_MESSAGE = 64;
    String ZEROS_64 = "0000000000000000000000000000000000000000000000000000000000000000";

    static boolean isAnEnum(Object v) {
        return (v instanceof Enum) || (v instanceof DynamicEnum);
    }

    /**
     * Write a boolean value.
     */
    @NotNull
    WireOut bool(Boolean flag);

    /**
     * Write a text value.
     */
    @NotNull
    WireOut text(@Nullable CharSequence s);

    /**
     * Write a text value. Delegates to {@link #text(CharSequence)}.
     */
    @NotNull
    default WireOut text(@Nullable String s) {
        return text((CharSequence) s);
    }

    /**
     * Write a null value.
     */
    @NotNull
    default WireOut nu11() {
        return text((CharSequence) null);
    }

    /**
     * Write a text value comprised of a single character.
     */
    @NotNull
    default WireOut text(char c) {
        try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
            return text(stlSb.get().append(c));
        }
    }

    /**
     * Write a character value. Delegates to {@link #text(CharSequence)}.
     */
    @NotNull
    default WireOut character(char c) {
        return text(c);
    }

    /**
     * Write a text value of {@link BytesStore} contents.
     */
    @NotNull
    default WireOut text(@Nullable BytesStore s) {
        return text((CharSequence) s);
    }

    /**
     * Write a signed 8-bit integer value. Delegates to {@link #int8(byte)}.
     *
     * @throws ArithmeticException if supplied argument does not fit in unsigned 8-bit.
     */
    @NotNull
    default WireOut int8(long x) {
        return int8(Maths.toInt8(x));
    }

    /**
     * Write a signed 8-bit integer value.
     */
    @NotNull
    WireOut int8(byte i8);

    /**
     * Write a byte sequence value from {@link BytesStore}.
     */
    @NotNull
    WireOut bytes(@Nullable BytesStore fromBytes);

    /**
     * Write a byte sequence value from {@link BytesStore} as a literal, if supported by wire type.
     * Defaults to {@link #bytes(BytesStore)}.
     */
    @NotNull
    default WireOut bytesLiteral(@Nullable BytesStore fromBytes) {
        return bytes(fromBytes);
    }

    /**
     * Write a typed bytes sequence value from {@link BytesStore}.
     */
    @NotNull
    WireOut bytes(String type, @Nullable BytesStore fromBytes);

    /**
     * Write a raw bytes sequence value. Behavior is implementation-dependent.
     */
    @NotNull
    WireOut rawBytes(byte[] value);

    /**
     * Write a raw text value. Behavior is implementation-dependent. Defaults to {@link #text(CharSequence)}.
     */
    @NotNull
    default WireOut rawText(CharSequence value) {
        return text(value);
    }

    /**
     * Writes value length if supported by implementation.
     */
    @NotNull
    ValueOut writeLength(long remaining);

    /**
     * Write a byte sequence value.
     */
    @NotNull
    WireOut bytes(byte[] fromBytes);

    /**
     * Write a typed byte sequence value.
     */
    @NotNull
    WireOut bytes(String type, byte[] fromBytes);

    /**
     * Write an unsigned 8-bit integer value.
     *
     * @throws ArithmeticException if supplied argument does not fit in unsigned 8-bit.
     */
    @NotNull
    default WireOut uint8(int x) {
        return uint8checked(Maths.toUInt8(x & 0xFF));
    }

    /**
     * Write an unsigned 8-bit integer value. The argument is assumed to be of correct bounds.
     */
    @NotNull
    WireOut uint8checked(int u8);

    /**
     * Write a signed 16-bit integer value.
     *
     * @throws ArithmeticException if supplied argument does not fit in signed 16-bit.
     */
    @NotNull
    default WireOut int16(long x) {
        return int16(Maths.toInt16(x));
    }

    /**
     * Write a signed 16-bit integer value. The argument is assumed to be of correct bounds.
     */
    @NotNull
    WireOut int16(short i16);

    /**
     * Write an unsigned 16-bit integer value. Delegates to {@link #uint16checked(int)}.
     */
    @NotNull
    default WireOut uint16(long x) {
        return uint16checked((int) x);
    }

    /**
     * Write an unsigned 16-bit integer value. The argument is assumed to be of correct bounds.
     */
    @NotNull
    WireOut uint16checked(int u16);

    /**
     * Write a single Java 16-bit Unicode codepoint. Behavior is implementation-dependent.
     */
    @NotNull
    WireOut utf8(int codepoint);

    /**
     * Write a signed 32-bit integer value.
     *
     * @throws ArithmeticException if supplied argument does not fit in signed 32-bit.
     */
    @NotNull
    default WireOut int32(long x) {
        return int32(Maths.toInt32(x));
    }

    /**
     * Write a signed 32-bit integer value. The argument is assumed to be of correct bounds.
     */
    @NotNull
    WireOut int32(int i32);

    @NotNull
    default WireOut int32(int i32, int previous) {
        return int32(i32);
    }

    /**
     * Write an unsigned 32-bit integer value. Delegates to {@link #uint32checked(long)}.
     */
    @NotNull
    default WireOut uint32(long x) {
        return uint32checked(x);
    }

    /**
     * Write an unsigned 32-bit integer value. The argument is assumed to be of correct bounds.
     */
    @NotNull
    WireOut uint32checked(long u32);

    /**
     * Write a signed 64-bit integer value.
     */
    @NotNull
    WireOut int64(long i64);

    @NotNull
    default WireOut int64(long i64, long previous) {
        return int64(i64);
    }

    @NotNull
    WireOut int128forBinding(long i64x0, long i64x1, TwoLongValue value);

    /**
     * Write a 64-bit integer as a hex value, if supported by wire type.
     */
    @NotNull
    WireOut int64_0x(long i64);

    @NotNull
    WireOut int64array(long capacity);

    /**
     * Write a 64-bit integer sequence value.
     */
    @NotNull
    WireOut int64array(long capacity, LongArrayValues values);

    /**
     * Write a 32-bit float value.
     */
    @NotNull
    WireOut float32(float f);

    /**
     * Write a 64-bit float (double) value.
     */
    @NotNull
    WireOut float64(double d);

    @NotNull
    default WireOut float32(float f, float previous) {
        return float32(f);
    }

    @NotNull
    default WireOut float64(double d, double previous) {
        return float64(d);
    }

    /**
     * Write a time value.
     */
    @NotNull
    WireOut time(LocalTime localTime);

    /**
     * Write a date time value with time zone.
     */
    @NotNull
    WireOut zonedDateTime(ZonedDateTime zonedDateTime);

    /**
     * Write a date value.
     */
    @NotNull
    WireOut date(LocalDate localDate);

    /**
     * Write a date time value.
     */
    @NotNull
    WireOut dateTime(LocalDateTime localDateTime);

    /**
     * Write a type prefix for a value.
     */
    @NotNull
    ValueOut typePrefix(CharSequence typeName);

    /**
     * Write a type prefix for a value of a specified {@link Class}.
     */
    @NotNull
    default ValueOut typePrefix(Class type) {
        return type == null ? this : typePrefix(classLookup().nameFor(type));
    }

    ClassLookup classLookup();

    /**
     * Write a type literal value of a specified {@link Class}.
     */
    @NotNull
    default WireOut typeLiteral(@Nullable Class type) {
        return type == null ? nu11()
                : typeLiteral((t, b) -> b.appendUtf8(classLookup().nameFor(t)), type);
    }

    /**
     * Write a type literal value of a specified {@link Type}.
     */
    @NotNull
    default WireOut typeLiteral(@Nullable Type type) {
        return type == null ? nu11()
                : type instanceof Class ? typeLiteral((Class) type)
                : typeLiteral(type.getTypeName());
    }

    /**
     * Write a type literal value.
     */
    @NotNull
    WireOut typeLiteral(@Nullable CharSequence type);

    /**
     * Write a type literal value using the specified type translator.
     */
    @NotNull
    WireOut typeLiteral(@NotNull BiConsumer<Class, Bytes<?>> typeTranslator, @Nullable Class type);

    /**
     * Write a UUID value.
     */
    @NotNull
    WireOut uuid(UUID uuid);

    @NotNull
    WireOut int32forBinding(int value);

    @NotNull
    WireOut int32forBinding(int value, @NotNull IntValue intValue);

    @NotNull
    WireOut int64forBinding(long value);

    @NotNull
    default WireOut int128forBinding(long value, long value2) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    WireOut int64forBinding(long value, @NotNull LongValue longValue);

    @NotNull
    WireOut boolForBinding(boolean value, @NotNull BooleanValue longValue);

    /**
     * Write a sequence value.
     */
    @NotNull
    default WireOut sequence(WriteValue writer) {
        return sequence(writer, WriteValue::writeValue);
    }

    /**
     * Write a sequence value from {@link Iterator}.
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
     * Write a sequence value using the provided writer.
     */
    @NotNull <T> WireOut sequence(T t, BiConsumer<T, ValueOut> writer);

    /**
     * Write a sequence value using the provided parametrized writer.
     */
    @NotNull <T, K> WireOut sequence(T t, K param, TriConsumer<T, K, ValueOut> writer) throws InvalidMarshallableException;

    /**
     * Write a sequence value of a specified length.
     */
    default <T> WireOut sequenceWithLength(T t, int length, ObjectIntObjectConsumer<T, ValueOut> writer) {
        boolean b = swapLeaf(true);
        WireOut sequence = sequence(t, length, writer::accept);
        swapLeaf(b);
        return sequence;
    }

    /**
     * Write an array of bytes sequences of a specified length.
     */
    default WireOut array(Bytes[] array, int length) {
        return sequenceWithLength(array, length, (a, len, out) -> {
            for (int i = 0; i < len; i++)
                out.bytes(a[i]);
        });
    }

    /**
     * Write an array of doubles of a specified length.
     */
    default WireOut array(double[] array, int length) {
        return sequenceWithLength(array, length, (a, len, out) -> {
            for (int i = 0; i < len; i++)
                out.float64(a[i]);
        });
    }

    /**
     * This write values relative to the first one using 6 digit precision
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
     * Write an array of booleans of a specified length.
     */
    default WireOut array(boolean[] array, int length) {
        return sequenceWithLength(array, length, (a, len, out) -> {
            for (int i = 0; i < len; i++)
                out.bool(a[i]);
        });
    }

    /**
     * Write an array of longs of a specified length.
     */
    default WireOut array(long[] array, int length) {
        return sequenceWithLength(array, length, (a, len, out) -> {
            for (int i = 0; i < len; i++)
                out.int64(a[i]);
        });
    }

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
     * Write an array of ints of a specified length.
     */
    default WireOut array(int[] array, int length) {
        return sequenceWithLength(array, length, (a, len, out) -> {
            for (int i = 0; i < len; i++)
                out.int32(a[i]);
        });
    }

    /**
     * Write an array of bytes of a specified length.
     */
    default WireOut array(byte[] array, int length) {
        return sequenceWithLength(array, length, (a, len, out) -> {
            for (int i = 0; i < len; i++)
                out.int8(a[i]);
        });
    }

    /**
     * Write an array of specified type objects.
     */
    @NotNull
    default WireOut array(@NotNull WriteValue writer, @NotNull Class arrayType) {
        if (arrayType == String[].class) {
            typePrefix("String[] ");
        } else if (arrayType != Object[].class) {
            typePrefix(classLookup().nameFor(arrayType.getComponentType()) + "[]");
        }
        return sequence(writer);
    }

    /**
     * Write a {@link WriteMarshallable} value.
     */
    @NotNull
    WireOut marshallable(WriteMarshallable object) throws InvalidMarshallableException;

    /**
     * Write a {@link Serializable} value.
     */
    @NotNull
    WireOut marshallable(Serializable object) throws InvalidMarshallableException;

    /**
     * writes the contents of the map to wire
     *
     * @param map a java map with, the key and value type of the map must be either Marshallable,
     *            String or Autoboxed primitives.
     * @return throws IllegalArgumentException  If the type of the map is not one of those listed above
     */
    @NotNull
    WireOut map(Map map) throws InvalidMarshallableException;

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

    default void endTypePrefix() {

    }

    /**
     * Write a {@link Serializable} value.
     * To be used when you know it is a typed serializable object.
     * If you are not sure, use the {@link #object(Object)} method.
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
            // Make sure we close the type scope
            endTypePrefix();
        }
    }

    /**
     * Write a {@link WriteMarshallable} value, prepending it with specified type prefix.
     */
    @NotNull
    default WireOut typedMarshallable(CharSequence typeName, WriteMarshallable object) throws InvalidMarshallableException {
        typePrefix(typeName);
        return marshallable(object);
    }

    /**
     * Write an enum value.
     */
    @NotNull
    default <E extends Enum<E>> WireOut asEnum(@Nullable E e) {
        return text(e == null ? null : e.name());
    }

    /**
     * Write a set (collection) value.
     */
    @NotNull
    default <V> WireOut set(Set<V> coll) throws InvalidMarshallableException {
        return set(coll, null);
    }

    /**
     * Write a set containing specified type of entries.
     */
    @NotNull
    default <V> WireOut set(Set<V> coll, Class<V> assumedClass) throws InvalidMarshallableException {
        return collection(coll, assumedClass);
    }

    /**
     * Write a list (collection) value.
     */
    @NotNull
    default <V> WireOut list(List<V> coll) throws InvalidMarshallableException {
        return list(coll, null);
    }

    /**
     * Write a list containing specified type of entries.
     */
    @NotNull
    default <V> WireOut list(List<V> coll, Class<V> assumedClass) throws InvalidMarshallableException {
        sequence(coll, assumedClass, (s, kls, out) -> {
            int size = s.size();
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < size; i++) {
                boolean wasLeaf = out.swapLeaf(true);
                marshallable((WriteMarshallable) s.get(i));
                out.swapLeaf(wasLeaf);
            }
        });
        return wireOut();
    }

    /**
     * Write a collection containing specified type of entries.
     */
    @NotNull
    default <V> WireOut collection(Collection<V> coll, Class<V> assumedClass) throws InvalidMarshallableException {
        sequence(coll, assumedClass, (s, kls, out) -> {
            for (V v : s) {
                object(kls, v);
            }
        });
        return wireOut();
    }

    /**
     * Write an object value of specified type.
     */
    @NotNull
    default <V> WireOut object(@NotNull Class<V> expectedType, V v) throws InvalidMarshallableException {
        Class<?> vClass = v == null ? void.class : v.getClass();
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
     * Write a map.
     */
    @NotNull
    default <K, V> WireOut marshallable(Map<K, V> map) throws InvalidMarshallableException {
        return marshallable(map, (Class) Object.class, (Class) Object.class, true);
    }

    /**
     * Write a map containing specified key and value typed objects.
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
     * Write an object value.
     */
    @NotNull
    default WireOut object(@Nullable Object value) throws InvalidMarshallableException {
        if (value == null)
            return nu11();
        // look for exact matches
        final Class<?> valueClass = value.getClass();
        switch (valueClass.getName()) {
            case "[B": {
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
        if (value instanceof WriteMarshallable) {
            if (isAnEnum(value)) {
                Jvm.debug().on(getClass(), "Treating " + valueClass + " as enum not WriteMarshallable");
                return typedScalar(value);
            }
            return Jvm.isLambdaClass(valueClass)
                    ? marshallable((WriteMarshallable) value)
                    : typedMarshallable((WriteMarshallable) value);
        }
        if (value instanceof WriteBytesMarshallable) {
            if (!Wires.warnedUntypedBytesOnce) {
                Jvm.warn().on(ValueOut.class, "BytesMarshallable found in field which is not matching exactly, " +
                        "the object may not unmarshall correctly if that type is not specified: " + valueClass.getName() +
                        ". The warning will not repeat so there may be more types affected.");

                Wires.warnedUntypedBytesOnce = true;
            }

            return bytesMarshallable((BytesMarshallable) value);
        }
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
            return sequence(v -> ((Collection) value).forEach(v::object));
        } else if (WireSerializedLambda.isSerializableLambda(valueClass)) {
            WireSerializedLambda.write(value, this);
            return wireOut();
        } else if (Object[].class.isAssignableFrom(valueClass)) {
            @NotNull Class type = valueClass.getComponentType();
            return array(v -> Stream.of((Object[]) value).forEach(val -> v.object(type, val)), valueClass);
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

            String typeName;
            try {
                typeName = Wires.typeNameFor(classLookup(), value);
            } catch (IllegalArgumentException e) {
                if (isBinary())
                    throw e;
                typeName = valueClass.getName();
            }
            if (typeName != null)
                typePrefix(typeName);
            marshallable(w -> Wires.writeMarshallable(value, w));
            if (typeName != null)
                endTypePrefix();
            return wireOut();
        }
    }

    default WireOut bytesMarshallable(WriteBytesMarshallable value) throws InvalidMarshallableException {
        throw new UnsupportedOperationException();
    }

    /**
     * Add an optional type i.e. if TEXT is used.
     *
     * @param aClass to write
     * @return this
     */
    @NotNull
    default ValueOut optionalTyped(Class aClass) {
        return this;
    }

    /**
     * Write a type-prefixed float value.
     */
    @NotNull
    default WireOut fixedFloat32(float value) {
        return typePrefix(float.class).float32(value);
    }

    /**
     * Write a type-prefixed signed 8-bit value.
     */
    @NotNull
    default WireOut fixedInt8(byte value) {
        return typePrefix(byte.class).int8(value);
    }

    /**
     * Write a type-prefixed signed 16-bit value.
     */
    @NotNull
    default WireOut fixedInt16(short value) {
        return typePrefix(short.class).int16(value);
    }

    /**
     * Write a type-prefixed signed 32-bit value.
     */
    @NotNull
    default WireOut fixedInt32(int value) {
        return typePrefix(int.class).int32(value);
    }

    /**
     * Write a type-prefixed double value.
     */
    @NotNull
    default WireOut fixedFloat64(double value) {
        return float64(value);
    }

    /**
     * Write a type-prefixed signed 64-bit value.
     */
    @NotNull
    default WireOut fixedInt64(long value) {
        return int64(value);
    }

    /**
     * Write an untyped object value.
     */
    @NotNull
    default WireOut untypedObject(@Nullable Object value) throws InvalidMarshallableException {
        if (value == null)
            return nu11();
        // look for exact matches
        switch (value.getClass().getName()) {
            case "[B":
                return bytes((byte[]) value);
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
            case "java.math.BigInteger":
            case "java.math.BigDecimal":
            case "java.io.File":
                return text(value.toString());
        }
        if (isAnEnum(value)) {
            String name = value instanceof DynamicEnum
                    ? ((DynamicEnum) value).name()
                    : ((Enum) value).name();
            return text(name);
        }
        if (value instanceof Marshallable)
            return marshallable((Marshallable) value);

        if (value instanceof WriteBytesMarshallable)
            return bytesMarshallable((BytesMarshallable) value);

        if (Object[].class.isAssignableFrom(value.getClass())) {
            @NotNull Class type = value.getClass().getComponentType();
            return array(v -> Stream.of((Object[]) value).forEach(val -> v.object(type, val)), Object[].class);
        }
        return object(value);
    }

    /**
     * Write an typed scalar value as type prefixed text.
     */
    @NotNull
    default WireOut typedScalar(@NotNull Object value) {
        typePrefix(Wires.typeNameFor(classLookup(), value));

        if (value instanceof Enum)
            value = ((Enum) value).name();
        if (value instanceof CoreDynamicEnum)
            value = ((CoreDynamicEnum) value).name();
        else if (!(value instanceof CharSequence))
            value = value.toString();

        text((CharSequence) value);
        endTypePrefix();
        return wireOut();
    }

    /**
     * Write a throwable value.
     */
    @NotNull
    default WireOut throwable(@NotNull Throwable t) throws InvalidMarshallableException {
        typedMarshallable(t.getClass().getName(), (WireOut w) -> {
            w.write("message").text(t.getMessage())
                    .write("stackTrace").sequence(w3 -> {
                        for (StackTraceElement ste : t.getStackTrace()) {
                            w3.marshallable(w4 ->
                                    w4.write("class").text(ste.getClassName())
                                            .write("method").text(ste.getMethodName())
                                            .write("file").text(ste.getFileName())
                                            .write("line").int32(ste.getLineNumber()));
                        }
                    });
            if (t.getCause() != null)
                w.write("cause").throwable(t.getCause());
        });
        return wireOut();
    }

    @NotNull
    WireOut wireOut();

    @NotNull
    default WireOut compress(@NotNull String compression, @Nullable Bytes<?> uncompressedBytes) {
        if (uncompressedBytes == null)
            return nu11();
        if (uncompressedBytes.readRemaining() < SMALL_MESSAGE)
            return bytes(uncompressedBytes);
        try (ScopedResource<Bytes<?>> stlBytes = Wires.acquireBytesScoped()) {
            Bytes<?> tmpBytes = stlBytes.get();
            Compression.compress(compression, uncompressedBytes, tmpBytes);
            bytes(compression, tmpBytes);
            return wireOut();
        }
    }

    default int compressedSize() {
        return Integer.MAX_VALUE;
    }

    default void resetBetweenDocuments() {
        resetState();
    }

    void resetState();

    /**
     * @return {@code true} if this wire type is binary wire.
     */
    default boolean isBinary() {
        return false;
    }

    /**
     * Write a boolean value. Delegates to {@link #bool(Boolean)}.
     */
    default WireOut writeBoolean(boolean x) {
        return bool(x);
    }

    /**
     * Write a byte value. Delegates to {@link #int8(byte)}.
     */
    default WireOut writeByte(byte x) {
        return int8(x);
    }

    /**
     * Write a char value. Delegates to {@link #uint16(long)}.
     */
    default WireOut writeChar(char x) {
        return uint16(x);
    }

    /**
     * Write a short value. Delegates to {@link #int16(long)}.
     */
    default WireOut writeShort(short x) {
        return int16(x);
    }

    /**
     * Write an int value. Delegates to {@link #int32(long)}.
     */
    default WireOut writeInt(int x) {
        return int32(x);
    }

    /**
     * Write a long value. Delegates to {@link #int64(long)}.
     */
    default WireOut writeLong(long x) {
        return int64(x);
    }

    /**
     * Write a float value. Delegates to {@link #float32(float)}.
     */
    default WireOut writeFloat(float x) {
        return float32(x);
    }

    /**
     * Write a double value. Delegates to {@link #float64(double)}.
     */
    default WireOut writeDouble(double x) {
        return float64(x);
    }

    /**
     * Write a string value. Delegates to {@link #text(CharSequence)}.
     */
    default WireOut writeString(CharSequence x) {
        return text(x);
    }

    /**
     * Write an int value with a specified converter.
     */
    default WireOut writeInt(LongConverter converter, int i) {
        try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
            StringBuilder sb = stlSb.get();
            converter.append(sb, i);
            return rawText(sb);
        }
    }

    /**
     * Write a long value with a specified converter.
     */
    default WireOut writeLong(LongConverter longConverter, long l) {
        try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
            StringBuilder sb = stlSb.get();
            longConverter.append(sb, l);
            if (longConverter.allSafeChars(wireOut()) && sb.length() > 0)
                return rawText(sb);
            else
                return text(sb);
        }
    }

    /**
     * This is a kludge and is here so that {@link WireMarshaller#of(Class)} detects this as not a leaf.
     * Previously this was a lambda, and {@link WireMarshaller#of(Class)} Java >= 15 falsely labelled it as a leaf
     */
    class MapMarshaller<K, V> implements WriteMarshallable {
        private Map<K, V> map;
        private Class<K> kClass;
        private Class<V> vClass;
        private boolean leaf;

        void params(@Nullable Map<K, V> map, @NotNull Class<K> kClass, @NotNull Class<V> vClass, boolean leaf) {
            this.map = map;
            this.kClass = kClass;
            this.vClass = vClass;
            this.leaf = leaf;
        }

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
