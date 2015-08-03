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
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by peter.lawrey on 14/01/15.
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
    ValueOut type(CharSequence typeName);

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
        type(ClassAliasPool.CLASS_ALIASES.nameFor(object.getClass()));
        return marshallable(object);
    }

    @NotNull
    default WireOut typedMarshallable(CharSequence typeName, WriteMarshallable object) {
        type(typeName);
        return marshallable(object);
    }

    @NotNull
    default WireOut object(Object value) {
        if (value instanceof byte[])
            return rawBytes((byte[]) value);
        if (value == null)
            return text(null);
        if (value instanceof Map)
            return map((Map) value);
        if (value instanceof Byte)
            return int8((Byte) value);
        else if (value instanceof Boolean)
            return bool((Boolean) value);
        else if (value instanceof Character)
            return text(value.toString());
        else if (value instanceof Short)
            return int16((Short) value);
        else if (value instanceof Integer)
            return int32((Integer) value);
        else if (value instanceof Long)
            return int64((Long) value);
        else if (value instanceof Double)
            return float64((Double) value);
        else if (value instanceof Float)
            return float32((Float) value);
        else if (value instanceof Marshallable)
            return typedMarshallable((Marshallable) value);
        else if (value instanceof Throwable)
            return throwable((Throwable) value);
        else if (value instanceof BytesStore)
            return bytes((BytesStore) value);
        else if (value instanceof CharSequence)
            return text((CharSequence) value);
        else if (value instanceof Enum)
            return typedScalar(value);
        else if (WireSerializedLambda.isSerializableLambda(value.getClass())) {
            WireSerializedLambda.write(value, this);
            return wireOut();
        } else {
            throw new IllegalStateException("type=" + value.getClass() +
                    " is unsupported, it must either be of type Marshallable, String or " +
                    "AutoBoxed primitive Object");
        }
    }

    @NotNull
    default WireOut typedScalar(@NotNull Object value) {
        type(ClassAliasPool.CLASS_ALIASES.nameFor(value.getClass()));
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
}