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
import net.openhft.chronicle.bytes.BytesStore;
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
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Write out data after writing a field.
 */
public interface ValueOut {
    /*
     * data types
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
    default WireOut typeLiteral(@NotNull Class type) {
        return typeLiteral((t, b) -> b.append(ClassAliasPool.CLASS_ALIASES.nameFor(t)), type);
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
    WireOut int32forBinding(int value, IntValue intValue);

    @NotNull
    WireOut int64forBinding(long value);

    @NotNull
    WireOut int64forBinding(long value, LongValue longValue);

    @NotNull
    WireOut sequence(Consumer<ValueOut> writer);

    @NotNull
    default WireOut array(Consumer<ValueOut> writer, Class arrayType) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    WireOut marshallable(WriteMarshallable object);

    /**
     * wites the contents of the map to wire
     *
     * @param map a java map with, the key and value type of the map must be either Marshallable,
     *            String or Autoboxed primitives.
     * @return throws IllegalArgumentException  If the type of the map is not one of those listed
     * above
     */
    @NotNull
    WireOut map(Map map);

    @NotNull
    WireOut typedMap(@NotNull Map<? extends WriteMarshallable, ? extends Marshallable> map);

    @NotNull
    ValueOut leaf();

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

    @NotNull
    default WireOut object(Object value) {
        if (value == null)
            return text(null);
        if (value instanceof Marshallable)
            return typedMarshallable((Marshallable) value);
        if (value instanceof BytesStore)
            return bytes((BytesStore) value);
        if (value instanceof CharSequence)
            return text((CharSequence) value);
        if (value instanceof Map)
            return map((Map) value);
        if (value instanceof byte[])
            return rawBytes((byte[]) value);
        if (value instanceof Byte)
            return int8((Byte) value);
        if (value instanceof Boolean)
            return bool((Boolean) value);
        if (value instanceof Character)
            return text(value.toString());
        if (value instanceof Short)
            return int16((Short) value);
        if (value instanceof Integer)
            return int32((Integer) value);
        if (value instanceof Long)
            return int64((Long) value);
        if (value instanceof Double)
            return float64((Double) value);
        if (value instanceof Float)
            return float32((Float) value);
        if (value instanceof Throwable)
            return throwable((Throwable) value);
        if (value instanceof Enum)
            return typedScalar(value);
        if (value instanceof String[])
            return array(v -> Stream.of((String[]) value).forEach(v::text), String[].class);
        if (value instanceof Collection) {
            if (((Collection) value).size() == 0) return sequence(v -> {
            });

            return sequence(v -> ((Collection) value).stream().forEach(v::object));
        } else if (WireSerializedLambda.isSerializableLambda(value.getClass())) {
            WireSerializedLambda.write(value, this);
            return wireOut();
        } else if (Object[].class.isAssignableFrom(value.getClass())) {
            return array(v -> Stream.of((Object[]) value).forEach(v::object), Object[].class);
        } else if (value instanceof LocalTime) {
            return time((LocalTime) value);
        } else if (value instanceof LocalDate) {
            return date((LocalDate) value);
        } else if (value instanceof ZonedDateTime) {
            return zonedDateTime((ZonedDateTime) value);
        } else {
            throw new IllegalStateException("type=" + value.getClass() +
                    " is unsupported, it must either be of type Marshallable, String or " +
                    "AutoBoxed primitive Object");
        }
    }

    @NotNull
    default WireOut typedScalar(@NotNull Object value) {
        typePrefix(ClassAliasPool.CLASS_ALIASES.nameFor(value.getClass()));
        text(value.toString());
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

    default WireOut compress(String compression, Bytes compressedBytes) {
        WireInternal.compress(this, compression, compressedBytes);
        return wireOut();
    }

    @Deprecated
    default WireOut compress(String compression, String str) {
        // replace with compress(String compression, Bytes compressedBytes)
        WireInternal.compress(this, compression, str);
        return wireOut();
    }

}