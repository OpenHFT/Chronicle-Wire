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
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.util.Compression;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.core.values.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/**
 * Write out data after writing a field.
 */
public interface ValueOut {

    int SMALL_MESSAGE = 64;
    String ZEROS_64 = "0000000000000000000000000000000000000000000000000000000000000000";

    /**
     * scalar data types
     */
    @NotNull
    WireOut bool(Boolean flag);

    @NotNull
    WireOut text(@Nullable CharSequence s);

    @NotNull
    default WireOut text(@Nullable String s) {
        return text((CharSequence) s);
    }

    @NotNull
    default WireOut nu11() {
        return text((CharSequence) null);
    }

    @NotNull
    default WireOut text(char c) {
        return text(Wires.acquireStringBuilder().append(c));
    }

    @NotNull
    default WireOut character(char c) {
        return text(c);
    }

    @NotNull
    default WireOut text(@Nullable BytesStore s) {
        return text((CharSequence) s);
    }

    @NotNull
    default WireOut int8(long x) {
        return int8(Maths.toInt8(x));
    }

    @NotNull
    WireOut int8(byte i8);

    @NotNull
    WireOut bytes(@Nullable BytesStore fromBytes);

    @NotNull
    default WireOut bytesLiteral(@Nullable BytesStore fromBytes) {
        return bytes(fromBytes);
    }

    @NotNull
    WireOut bytes(String type, @Nullable BytesStore fromBytes);

    @NotNull
    WireOut rawBytes(byte[] value);

    @NotNull
    default WireOut rawText(CharSequence value) {
        return text(value);
    }

    @NotNull
    ValueOut writeLength(long remaining);

    @NotNull
    WireOut bytes(byte[] fromBytes);

    @NotNull
    WireOut bytes(String type, byte[] fromBytes);

    @NotNull
    default WireOut uint8(int x) {
        return uint8checked((int) Maths.toUInt8(x));
    }

    @NotNull
    WireOut uint8checked(int u8);

    @NotNull
    default WireOut int16(long x) {
        return int16(Maths.toInt16(x));
    }

    @NotNull
    WireOut int16(short i16);

    @NotNull
    default WireOut uint16(long x) {
        return uint16checked((int) x);
    }

    @NotNull
    WireOut uint16checked(int u16);

    @NotNull
    WireOut utf8(int codepoint);

    @NotNull
    default WireOut int32(long x) {
        return int32(Maths.toInt32(x));
    }

    @NotNull
    WireOut int32(int i32);

    @NotNull
    default WireOut int32(int i32, int previous) {
        return int32(i32);
    }

    @NotNull
    default WireOut uint32(long x) {
        return uint32checked(x);
    }

    @NotNull
    WireOut uint32checked(long u32);

    @NotNull
    WireOut int64(long i64);

    @NotNull
    default WireOut int64(long i64, long previous) {
        return int64(i64);
    }

    @NotNull
    WireOut int128forBinding(long i64x0, long i64x1, TwoLongValue value);

    @NotNull
    WireOut int64_0x(long i64);

    @NotNull
    WireOut int64array(long capacity);

    @NotNull
    WireOut int64array(long capacity, LongArrayValues values);

    @NotNull
    WireOut float32(float f);

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

    @NotNull
    WireOut time(LocalTime localTime);

    @NotNull
    WireOut zonedDateTime(ZonedDateTime zonedDateTime);

    @NotNull
    WireOut date(LocalDate localDate);

    @NotNull
    WireOut dateTime(LocalDateTime localDateTime);

    @NotNull
    ValueOut typePrefix(CharSequence typeName);

    @NotNull
    default ValueOut typePrefix(Class type) {
        return type == null ? this : typePrefix(ClassAliasPool.CLASS_ALIASES.nameFor(type));
    }

    @NotNull
    default WireOut typeLiteral(@Nullable Class type) {
        return type == null ? nu11()
                : typeLiteral((t, b) -> b.appendUtf8(ClassAliasPool.CLASS_ALIASES.nameFor(t)), type);
    }

    @NotNull
    WireOut typeLiteral(@Nullable CharSequence type);

    @NotNull
    WireOut typeLiteral(@NotNull BiConsumer<Class, Bytes> typeTranslator, @Nullable Class type);

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

    @NotNull
    default WireOut sequence(WriteValue writer) {
        return sequence(writer, WriteValue::writeValue);
    }

    @NotNull
    default <T> WireOut sequence(Iterable<T> t) {
        if (t instanceof SortedSet)
            typePrefix(SortedSet.class);
        else if (t instanceof Set)
            typePrefix(Set.class);
        return sequence(t, (it, out) -> {
            for (T o : it) {
                out.object(o);
            }
        });
    }

    @NotNull
    <T> WireOut sequence(T t, BiConsumer<T, ValueOut> writer);

    @NotNull
    <T, K> WireOut sequence(T t, K kls, TriConsumer<T, K, ValueOut> writer);

    default <T, K> WireOut sequenceWithLength(T t, int length, ObjectIntObjectConsumer<T, ValueOut> writer) {
        boolean b = swapLeaf(true);
        WireOut sequence = sequence(t, length, writer::accept);
        swapLeaf(b);
        return sequence;
    }

    default WireOut array(Bytes[] array, int length) {
        return sequenceWithLength(array, length, (a, len, out) -> {
            for (int i = 0; i < len; i++)
                out.bytes(a[i]);
        });
    }

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

    default WireOut array(boolean[] array, int length) {
        return sequenceWithLength(array, length, (a, len, out) -> {
            for (int i = 0; i < len; i++)
                out.bool(a[i]);
        });
    }

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

    default WireOut array(int[] array, int length) {
        return sequenceWithLength(array, length, (a, len, out) -> {
            for (int i = 0; i < len; i++)
                out.int32(a[i]);
        });
    }

    default WireOut array(byte[] array, int length) {
        return sequenceWithLength(array, length, (a, len, out) -> {
            for (int i = 0; i < len; i++)
                out.int8(a[i]);
        });
    }

    @NotNull
    default WireOut array(@NotNull WriteValue writer, @NotNull Class arrayType) {
        if (arrayType == String[].class) {
            typePrefix("String[] ");
        } else if (arrayType != Object[].class) {
            typePrefix(ClassAliasPool.CLASS_ALIASES.nameFor(arrayType.getComponentType()) + "[]");
        }
        return sequence(writer);
    }

    @NotNull
    WireOut marshallable(WriteMarshallable object);

    @NotNull
    WireOut marshallable(Serializable object);

    /**
     * writes the contents of the map to wire
     *
     * @param map a java map with, the key and value type of the map must be either Marshallable,
     *            String or Autoboxed primitives.
     * @return throws IllegalArgumentException  If the type of the map is not one of those listed
     * above
     * @deprecated use marshallable(map) or object(map)
     */
    @NotNull
    @Deprecated
    WireOut map(Map map);

    /**
     * @deprecated use typedMarshallable(map) or object(map)
     */
    @NotNull
    @Deprecated
    WireOut typedMap(@NotNull Map<? extends WriteMarshallable, ? extends Marshallable> map);

    @NotNull
    @Deprecated
    default ValueOut leaf() {
        swapLeaf(true);
        return this;
    }

    @NotNull
    @Deprecated
    default ValueOut leaf(boolean leaf) {
        swapLeaf(leaf);
        return this;
    }

    @NotNull
    default boolean swapLeaf(boolean isLeaf) {
        return false;
    }

    /**
     * To be used when you know it is a typed marshallable object. e.g. an Enum is not, it's a scalar. If you are not sure, use the object(x) method.
     *
     * @param marshallable to write
     * @return the original wire
     */
    @NotNull
    default WireOut typedMarshallable(@Nullable WriteMarshallable marshallable) {
        if (marshallable == null)
            return nu11();
        String typeName = Wires.typeNameFor(marshallable);
        if (typeName != null)
            typePrefix(typeName);
        return marshallable(marshallable);
    }

    @NotNull
    default WireOut typedMarshallable(@Nullable Serializable object) {
        if (object == null)
            return nu11();
        typePrefix(object.getClass());
        if (object instanceof WriteMarshallable) {
            return marshallable((WriteMarshallable) object);
        } else {
            return marshallable(object);
        }
    }

    @NotNull
    default WireOut typedMarshallable(CharSequence typeName, WriteMarshallable object) {
        typePrefix(typeName);
        return marshallable(object);
    }

    @NotNull
    default <E extends Enum<E>> WireOut asEnum(@Nullable E e) {
        return text(e == null ? null : e.name());
    }

    @NotNull
    default <V> WireOut set(Set<V> coll) {
        return set(coll, null);
    }

    @NotNull
    default <V> WireOut set(Set<V> coll, Class<V> assumedClass) {
        return collection(coll, assumedClass);
    }

    @NotNull
    default <V> WireOut list(List<V> coll) {
        return list(coll, null);
    }

    @NotNull
    default <V> WireOut list(List<V> coll, Class<V> assumedClass) {
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

    @NotNull
    default <V> WireOut collection(Collection<V> coll, Class<V> assumedClass) {
        sequence(coll, assumedClass, (s, kls, out) -> {
            for (V v : s) {
                object(kls, v);
            }
        });
        return wireOut();
    }

    @NotNull
    default <V> WireOut object(@NotNull Class<V> expectedType, V v) {
        if (v instanceof WriteMarshallable && !(v instanceof Enum))
            if (ObjectUtils.matchingClass(expectedType, v.getClass()))
                marshallable((WriteMarshallable) v);
            else
                typedMarshallable((WriteMarshallable) v);
        else if (v != null && ObjectUtils.matchingClass(expectedType, v.getClass()))
            untypedObject(v);
        else
            object(v);
        return wireOut();
    }

    @NotNull
    default <K, V> WireOut marshallable(Map<K, V> map) {
        return marshallable(map, (Class) Object.class, (Class) Object.class, true);
    }

    @NotNull
    default <K, V> WireOut marshallable(@Nullable Map<K, V> map, @NotNull Class<K> kClass, @NotNull Class<V> vClass, boolean leaf) {
        if (map == null) {
            nu11();
            return wireOut();
        }

        marshallable(m -> {
            for (@NotNull Map.Entry<K, V> entry : map.entrySet()) {
                ValueOut valueOut = m.writeEvent(kClass, entry.getKey());
                boolean wasLeaf = valueOut.swapLeaf(leaf);
                valueOut.object(vClass, entry.getValue());
                valueOut.swapLeaf(wasLeaf);
            }
        });
        return wireOut();
    }

    @NotNull
    default WireOut object(@Nullable Object value) {
        if (value == null)
            return nu11();
        // look for exact matches
        switch (value.getClass().getName()) {
            case "[B":
                return typePrefix(byte[].class).bytes((byte[]) value);
            case "[S":
            case "[C":
            case "[I":
            case "[J":
            case "[F":
            case "[D":
            case "[Z":
                ValueOut valueOut = typePrefix(value.getClass());
                boolean wasLeaf = valueOut.swapLeaf(true);
                valueOut.sequence(value, (v, out) -> {
                    int len = Array.getLength(v);
                    for (int i = 0; i < len; i++) {
                        out.untypedObject(Array.get(v, i));
                    }
                });
                valueOut.swapLeaf(wasLeaf);
                return wireOut();

            case "net.openhft.chronicle.wire.RawText":
                return rawText(((RawText) value).text);
            case "java.lang.String":
                return text((String) value);
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
            case "[java.lang.String;":
                return array(v -> Stream.of((String[]) value).forEach(v::text), String[].class);
            case "[java.lang.Object;":
                return array(v -> Stream.of((Object[]) value).forEach(v::object), Object[].class);
            case "java.time.LocalTime":
                return optionalTyped(LocalTime.class).time((LocalTime) value);
            case "java.time.LocalDate":
                return optionalTyped(LocalDate.class).date((LocalDate) value);
            case "java.time.LocalDateTime":
                return optionalTyped(LocalDateTime.class).dateTime((LocalDateTime) value);
            case "java.time.ZonedDateTime":
                return optionalTyped(ZonedDateTime.class).zonedDateTime((ZonedDateTime) value);
            case "java.util.Date":
                long time = ((Date) value).getTime();
                return typePrefix(value.getClass()).fixedInt64(time);
            case "java.util.UUID":
                return optionalTyped(UUID.class).uuid((UUID) value);
            case "java.math.BigInteger":
            case "java.math.BigDecimal":
            case "java.io.File":
                return optionalTyped(value.getClass()).text(value.toString());
        }
        if (value instanceof WriteMarshallable) {
            if (value instanceof Enum) {
                Jvm.warn().on(getClass(), "Treating " + value.getClass() + " as enum not WriteMarshallable");
                return typedScalar(value);
            }
            return typedMarshallable((WriteMarshallable) value);
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
        if (value instanceof Enum)
            return typedScalar(value);
        if (value instanceof BitSet) {
            typePrefix(BitSet.class);

            BitSet bs = (BitSet) value;
            boolean isTextWire = TextWire.TextValueOut.class.isAssignableFrom(this.getClass());

            // note : this like the others below this is capturing lambda
            return sequence(v -> {
                for (int i = 0; i < bs.size() >> 6; i++) {
                    long l = BitSetUtil.getWord(bs, i);
                    WireOut wireOut = v.int64(l);
                    if (isTextWire) {
                        String bits = Long.toBinaryString(l);
                        wireOut.writeComment(ZEROS_64.substring(bits.length()) + bits);
                    }

                }
            });

        }
        if (value instanceof Collection) {
            if (value instanceof SortedSet)
                typePrefix(SortedSet.class);
            else if (value instanceof Set)
                typePrefix(Set.class);
            return sequence(v -> ((Collection) value).forEach(v::object));
        } else if (WireSerializedLambda.isSerializableLambda(value.getClass())) {
            WireSerializedLambda.write(value, this);
            return wireOut();
        } else if (Object[].class.isAssignableFrom(value.getClass())) {
            @NotNull Class type = value.getClass().getComponentType();
            return array(v -> Stream.of((Object[]) value).forEach(val -> v.object(type, val)), value.getClass());
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
        } else {
            throw new IllegalStateException("type=" + value.getClass() +
                    " is unsupported, it must either be of type Marshallable, String or " +
                    "AutoBoxed primitive Object");
        }
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

    @NotNull
    default WireOut fixedFloat32(float value) {
        return typePrefix(float.class).float32(value);
    }

    @NotNull
    default WireOut fixedInt8(byte value) {
        return typePrefix(byte.class).int8(value);
    }

    @NotNull
    default WireOut fixedInt16(short value) {
        return typePrefix(short.class).int16(value);
    }

    @NotNull
    default WireOut fixedInt32(int value) {
        return typePrefix(int.class).int32(value);
    }

    @NotNull
    default WireOut fixedFloat64(double value) {
        return float64(value);
    }

    @NotNull
    default WireOut fixedInt64(long value) {
        return int64(value);
    }

    @NotNull
    default WireOut untypedObject(@Nullable Object value) {
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
        if (value instanceof Enum)
            return text(((Enum) value).name());
        if (value instanceof Marshallable)
            return marshallable((Marshallable) value);
        if (Object[].class.isAssignableFrom(value.getClass())) {
            @NotNull Class type = value.getClass().getComponentType();
            return array(v -> Stream.of((Object[]) value).forEach(val -> v.object(type, val)), Object[].class);
        }
        return object(value);
    }

    @NotNull
    default WireOut typedScalar(@NotNull Object value) {
        typePrefix(Wires.typeNameFor(value));

        if (value instanceof Enum)
            value = ((Enum) value).name();
        else if (!(value instanceof CharSequence))
            value = value.toString();

        text((CharSequence) value);
        return wireOut();
    }

    @NotNull
    default WireOut throwable(@NotNull Throwable t) {
        typedMarshallable(t.getClass().getName(), (WireOut w) -> {
            w.write(() -> "message").text(t.getMessage())
                    .write(() -> "stackTrace").sequence(w3 -> {
                for (StackTraceElement ste : t.getStackTrace()) {
                    w3.marshallable(w4 ->
                            w4.write(() -> "class").text(ste.getClassName())
                                    .write(() -> "method").text(ste.getMethodName())
                                    .write(() -> "file").text(ste.getFileName())
                                    .write(() -> "line").int32(ste.getLineNumber()));
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
    default WireOut compress(@NotNull String compression, @Nullable Bytes uncompressedBytes) {
        if (uncompressedBytes == null)
            return nu11();
        if (uncompressedBytes.readRemaining() < SMALL_MESSAGE)
            return bytes(uncompressedBytes);
        Bytes tmpBytes = Wires.acquireBytes();
        Compression.compress(compression, uncompressedBytes, tmpBytes);
        bytes(compression, tmpBytes);
        return wireOut();
    }

    default int compressedSize() {
        return Integer.MAX_VALUE;
    }

    @NotNull
    @Deprecated
    default WireOut compress(@NotNull String compression, @Nullable String str) {
        if (str == null || str.length() < SMALL_MESSAGE)
            return text(str);
        // replace with compress(String compression, Bytes compressedBytes)
        WireInternal.compress(this, compression, str);
        return wireOut();
    }

    default void resetBetweenDocuments() {
        resetState();
    }

    void resetState();

}