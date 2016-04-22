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
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
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

    /**
     * scalar data types
     */
    @NotNull
    WireOut bool(Boolean flag);

    @NotNull
    WireOut text(@Nullable CharSequence s);

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
    WireOut bytes(String type, @Nullable BytesStore fromBytes);

    @NotNull
    WireOut rawBytes(byte[] value);

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
    default WireOut uint32(long x) {
        return uint32checked(x);
    }

    @NotNull
    WireOut uint32checked(long u32);

    @NotNull
    WireOut int64(long i64);

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
    WireOut time(LocalTime localTime);

    @NotNull
    WireOut zonedDateTime(ZonedDateTime zonedDateTime);

    @NotNull
    WireOut date(LocalDate localDate);

    @NotNull
    ValueOut typePrefix(CharSequence typeName);

    @NotNull
    default ValueOut typePrefix(Class type) {
        return typePrefix(ClassAliasPool.CLASS_ALIASES.nameFor(type));
    }

    @NotNull
    default WireOut typeLiteral(Class type) {
        return type == null ? text(null)
                : typeLiteral((t, b) -> b.appendUtf8(ClassAliasPool.CLASS_ALIASES.nameFor(t)), type);
    }

    @NotNull
    WireOut typeLiteral(@NotNull CharSequence type);

    @NotNull
    WireOut typeLiteral(@NotNull BiConsumer<Class, Bytes> typeTranslator, @NotNull Class type);

    @NotNull
    WireOut uuid(UUID uuid);

    @NotNull
    WireOut int32forBinding(int value);

    @NotNull
    WireOut int32forBinding(int value, @NotNull IntValue intValue);

    @NotNull
    WireOut int64forBinding(long value);

    @NotNull
    WireOut int64forBinding(long value, @NotNull LongValue longValue);

    @NotNull
    default WireOut sequence(WriteValue writer) {
        return sequence(writer, WriteValue::writeValue);
    }

    @NotNull
    <T> WireOut sequence(T t, BiConsumer<T, ValueOut> writer);

    @NotNull
    default WireOut array(@NotNull WriteValue writer, Class arrayType) {
        if (arrayType == String[].class) {
            typePrefix("String[] ");
        } else if (arrayType != Object[].class) {
            typePrefix(ClassAliasPool.CLASS_ALIASES.nameFor(arrayType.getComponentType()) + "[]");
        }
        return sequence(writer);
    }

    @NotNull
    WireOut marshallable(WriteMarshallable object);

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
    ValueOut leaf();

    default ValueOut leaf(boolean leaf) {
        return leaf ? leaf() : this;
    }

    @NotNull
    default WireOut typedMarshallable(@Nullable WriteMarshallable object) {
        if (object == null)
            return text(null);
        typePrefix(object.getClass());
        return marshallable(object);
    }

    @NotNull
    default WireOut typedMarshallable(CharSequence typeName, WriteMarshallable object) {
        typePrefix(typeName);
        return marshallable(object);
    }

    default <E extends Enum<E>> WireOut asEnum(E e) {
        return text(e == null ? null : e.name());
    }

    default <V> WireOut set(Set<V> coll) {
        return set(coll, null);
    }

    default <V> WireOut set(Set<V> coll, Class<V> assumedClass) {
        return collection(coll, assumedClass);
    }

    default <V> WireOut list(List<V> coll) {
        return list(coll, null);
    }

    default <V> WireOut list(List<V> coll, Class<V> assumedClass) {
        return collection(coll, assumedClass);
    }

    default <V> WireOut collection(Collection<V> coll, Class<V> assumedClass) {
        sequence(coll, (s, out) -> {
            for (V v : s) {
                out.leaf();
                object(assumedClass, v);
            }
        });
        return wireOut();
    }

    default <V> void object(Class<V> assumedClass, V v) {
        if (v instanceof WriteMarshallable)
            if (v.getClass() == assumedClass)
                marshallable((WriteMarshallable) v);
            else
                typedMarshallable((WriteMarshallable) v);
        else if (v != null && v.getClass() == assumedClass)
            untypedObject(v);
        else
            object(v);
    }

    default <V> WireOut marshallable(Map<String, V> map) {
        return marshallable(map, null);
    }

    default <V> WireOut marshallable(Map<String, V> map, Class<V> vClass) {
        return marshallable(map, vClass, true);
    }

    default <V> WireOut marshallable(Map<String, V> map, Class<V> vClass, boolean leaf) {
        marshallable(m -> {
            for (Map.Entry<String, V> entry : map.entrySet()) {
                m.writeEventName(entry::getKey).leaf(leaf).object(vClass, entry.getValue());
            }
        });
        return wireOut();
    }

    @NotNull
    default WireOut object(Object value) {
        if (value == null)
            return text(null);
        // look for exact matches
        switch (value.getClass().getName()) {
            case "[B":
                return typePrefix(byte[].class).bytes((byte[]) value);
            case "java.lang.String":
                return text((String) value);
            case "java.lang.Byte":
                return typePrefix(byte.class).int8((Byte) value);
            case "java.lang.Boolean":
                return bool((Boolean) value);
            case "java.lang.Character":
                return text(value.toString());
            case "java.lang.Short":
                return typePrefix(short.class).int16((Short) value);
            case "java.lang.Integer":
                return typePrefix(int.class).int32((Integer) value);
            case "java.lang.Long":
                return fixedInt64((Long) value);
            case "java.lang.Double":
                return fixedFloat64((Double) value);
            case "java.lang.Float":
                return typePrefix(float.class).float32((Float) value);
            case "[java.lang.String;":
                return array(v -> Stream.of((String[]) value).forEach(v::text), String[].class);
            case "[java.lang.Object;":
                return array(v -> Stream.of((Object[]) value).forEach(val -> v.object(val)), Object[].class);
            case "java.time.LocalTime":
                return time((LocalTime) value);
            case "java.time.LocalDate":
                return date((LocalDate) value);
            case "java.time.ZonedDateTime":
                return zonedDateTime((ZonedDateTime) value);
        }
        if (value instanceof Marshallable)
            return typedMarshallable((Marshallable) value);
        if (value instanceof BytesStore)
            return bytes((BytesStore) value);
        if (value instanceof CharSequence)
            return text((CharSequence) value);
        if (value instanceof Map)
            return map((Map) value);
        if (value instanceof Throwable)
            return throwable((Throwable) value);
        if (value instanceof Enum)
            return typedScalar(value);
        if (value instanceof Collection) {
            return sequence(v -> ((Collection) value).stream().forEach(v::object));
        } else if (WireSerializedLambda.isSerializableLambda(value.getClass())) {
            WireSerializedLambda.write(value, this);
            return wireOut();
        } else if (Object[].class.isAssignableFrom(value.getClass())) {
            Class type = (Class) value.getClass().getComponentType();
            return array(v -> Stream.of((Object[]) value).forEach(val -> v.object(type, val)), value.getClass());
        } else {
            throw new IllegalStateException("type=" + value.getClass() +
                    " is unsupported, it must either be of type Marshallable, String or " +
                    "AutoBoxed primitive Object");
        }
    }

    default WireOut fixedFloat64(double value) {
        return float64(value);
    }

    default WireOut fixedInt64(long value) {
        return int64(value);
    }

    @NotNull
    default WireOut untypedObject(Object value) {
        if (value == null)
            return text(null);
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
        }
        if (value instanceof Marshallable)
            return marshallable((Marshallable) value);
        if (value instanceof Enum)
            return text(((Enum) value).name());
        if (Object[].class.isAssignableFrom(value.getClass())) {
            Class type = (Class) value.getClass().getComponentType();
            return array(v -> Stream.of((Object[]) value).forEach(val -> v.object(type, val)), Object[].class);
        }
        return object(value);
    }

    @NotNull
    default WireOut typedScalar(@NotNull Object value) {
        typePrefix(ClassAliasPool.CLASS_ALIASES.nameFor(value.getClass()));
        if (!(value instanceof CharSequence))
            value = value.toString();

        text((CharSequence) value);
        return wireOut();
    }

    @NotNull
    default WireOut throwable(@NotNull Throwable t) {
        typedMarshallable(t.getClass().getName(), (WireOut w) ->
                w.write(() -> "message").text(t.getMessage())
                        .write(() -> "stackTrace").sequence(w3 -> {
                    StackTraceElement[] stes = t.getStackTrace();
                    int last = Jvm.trimLast(0, stes);
                    for (int i = 0; i < last; i++) {
                        StackTraceElement ste = stes[i];
                        w3.leaf().marshallable(w4 ->
                                w4.write(() -> "class").text(ste.getClassName())
                                        .write(() -> "method").text(ste.getMethodName())
                                        .write(() -> "file").text(ste.getFileName())
                                        .write(() -> "line").int32(ste.getLineNumber()));
                    }
                }));
        return wireOut();
    }

    @NotNull
    WireOut wireOut();

    default WireOut compress(String compression, Bytes uncompressedBytes) {
        if (uncompressedBytes == null)
            return text(null);
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

    @Deprecated
    default WireOut compress(String compression, String str) {
        if (str == null || str.length() < SMALL_MESSAGE)
            return text(str);
        // replace with compress(String compression, Bytes compressedBytes)
        WireInternal.compress(this, compression, str);
        return wireOut();
    }

}