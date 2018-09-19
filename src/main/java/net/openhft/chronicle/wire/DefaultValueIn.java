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

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.*;
import net.openhft.chronicle.core.values.BooleanValue;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferUnderflowException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.*;

/*
 * Created by peter.lawrey on 02/02/2016.
 */
public class DefaultValueIn implements ValueIn {
    private final WireIn wireIn;
    Object defaultValue;

    DefaultValueIn(WireIn wireIn) {
        this.wireIn = wireIn;
    }

    @Nullable
    @Override
    public String text() {
        @Nullable Object o = defaultValue;
        return o == null ? null : o.toString();
    }

    @Nullable
    @Override
    public StringBuilder textTo(@NotNull StringBuilder sb) {
        @Nullable Object o = defaultValue;
        if (o == null)
            return null;
        sb.append(o);
        return sb;
    }

    @Nullable
    @Override
    public Bytes textTo(@NotNull Bytes bytes) {
        @Nullable Object o = defaultValue;
        if (o == null)
            return null;
        bytes.write((BytesStore) o);
        return bytes;
    }

    @NotNull
    @Override
    public WireIn bytes(@NotNull BytesOut toBytes) {
        @Nullable Object o = defaultValue;
        if (o == null)
            return wireIn();
        @NotNull BytesStore bytes = (BytesStore) o;
        toBytes.write(bytes);
        return wireIn();
    }

    @Nullable
    @Override
    public WireIn bytesSet(@NotNull PointerBytesStore toBytes) {
        @Nullable Object o = defaultValue;
        if (o == null) {
            toBytes.set(NoBytesStore.NO_PAGE, 0);
            return wireIn();
        }
        @NotNull BytesStore bytes = (BytesStore) o;
        toBytes.set(bytes.addressForRead(0), bytes.realCapacity());
        return wireIn();
    }

    @NotNull
    @Override
    public WireIn bytesMatch(@NotNull BytesStore compareBytes, @NotNull BooleanConsumer consumer) {
        @Nullable Object o = defaultValue;
        @NotNull BytesStore bytes = (BytesStore) o;
        consumer.accept(compareBytes.contentEquals(bytes));
        return wireIn();
    }

    @NotNull
    @Override
    public WireIn bytes(@NotNull ReadBytesMarshallable wireInConsumer) {
        @Nullable Object o = defaultValue;
        if (o == null) {
            wireInConsumer.readMarshallable(Wires.NO_BYTES);
            return wireIn();
        }
        @Nullable BytesStore bytes = (BytesStore) o;
        wireInConsumer.readMarshallable(bytes.bytesForRead());
        return wireIn();
    }

    @Nullable
    @Override
    public byte[] bytes() {
        return (byte[]) defaultValue;
    }

    @NotNull
    @Override
    public WireIn wireIn() {
        return wireIn;
    }

    @Override
    public long readLength() {
        return 0;
    }

    @NotNull
    @Override
    public WireIn skipValue() {
        return wireIn();
    }

    @NotNull
    @Override
    public <T> WireIn bool(T t, @NotNull ObjBooleanConsumer<T> tFlag) {
        @Nullable Boolean o = (Boolean) defaultValue;
        tFlag.accept(t, o);
        return wireIn();
    }

    @NotNull
    @Override
    public <T> WireIn int8(@NotNull T t, @NotNull ObjByteConsumer<T> tb) {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        tb.accept(t, o.byteValue());
        return wireIn();
    }

    @NotNull
    @Override
    public <T> WireIn uint8(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        ti.accept(t, o.shortValue());
        return wireIn();
    }

    @NotNull
    @Override
    public <T> WireIn int16(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        ti.accept(t, o.shortValue());
        return wireIn();
    }

    @NotNull
    @Override
    public <T> WireIn uint16(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        ti.accept(t, o.intValue());
        return wireIn();
    }

    @NotNull
    @Override
    public <T> WireIn int32(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
        @Nullable Number o = (Number) defaultValue;
        if (o == null) o = 0;
        ti.accept(t, o.intValue());
        return wireIn();
    }

    @NotNull
    @Override
    public <T> WireIn uint32(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        tl.accept(t, o.longValue());
        return wireIn();
    }

    @NotNull
    @Override
    public <T> WireIn int64(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        tl.accept(t, o.longValue());
        return wireIn();
    }

    @NotNull
    @Override
    public <T> WireIn float32(@NotNull T t, @NotNull ObjFloatConsumer<T> tf) {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        tf.accept(t, o.floatValue());
        return wireIn();
    }

    @NotNull
    @Override
    public <T> WireIn float64(@NotNull T t, @NotNull ObjDoubleConsumer<T> td) {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        td.accept(t, o.doubleValue());
        return wireIn();
    }

    @NotNull
    @Override
    public <T> WireIn time(@NotNull T t, @NotNull BiConsumer<T, LocalTime> setLocalTime) {
        @Nullable LocalTime o = (LocalTime) defaultValue;
        setLocalTime.accept(t, o);
        return wireIn();
    }

    @NotNull
    @Override
    public <T> WireIn zonedDateTime(@NotNull T t, @NotNull BiConsumer<T, ZonedDateTime> tZonedDateTime) {
        @Nullable ZonedDateTime o = (ZonedDateTime) defaultValue;
        tZonedDateTime.accept(t, o);
        return wireIn();
    }

    @NotNull
    @Override
    public <T> WireIn date(@NotNull T t, @NotNull BiConsumer<T, LocalDate> tLocalDate) {
        @NotNull LocalDate o = (LocalDate) defaultValue;
        tLocalDate.accept(t, o);
        return wireIn();
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public boolean hasNextSequenceItem() {
        return false;
    }

    @NotNull
    @Override
    public <T> WireIn uuid(@NotNull T t, @NotNull BiConsumer<T, UUID> tuuid) {
        @NotNull UUID o = (UUID) defaultValue;
        tuuid.accept(t, o);
        return wireIn();
    }

    @NotNull
    @Override
    public <T> WireIn int64array(@Nullable LongArrayValues values, T t, @NotNull BiConsumer<T, LongArrayValues> setter) {
        throw new UnsupportedOperationException("todo");
    }

    @NotNull
    @Override
    public WireIn int64(@NotNull LongValue value) {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        value.setValue(o.longValue());
        return wireIn();
    }

    @NotNull
    @Override
    public WireIn int32(@NotNull IntValue value) {
        @Nullable Number o = (Number) defaultValue;
        if (o == null) o = 0;
        value.setValue(o.intValue());
        return wireIn();
    }

    @Override
    public WireIn bool(@NotNull final BooleanValue ret) {
        throw new UnsupportedOperationException("todo");
    }

    @NotNull
    @Override
    public <T> WireIn int64(@Nullable LongValue value, T t, @NotNull BiConsumer<T, LongValue> setter) {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        value.setValue(o.longValue());
        setter.accept(t, value);
        return wireIn();
    }

    @NotNull
    @Override
    public <T> WireIn int32(@Nullable IntValue value, T t, @NotNull BiConsumer<T, IntValue> setter) {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        value.setValue(o.intValue());
        setter.accept(t, value);
        return wireIn();
    }

    @NotNull
    @Override
    public <T> boolean sequence(@NotNull T t, @NotNull BiConsumer<T, ValueIn> tReader) {
        return false;
    }

    @Override
    public <T> boolean sequence(List<T> list, @NotNull List<T> buffer, Supplier<T> bufferAdd, Reader reader0) {
        return false;
    }

    @NotNull
    @Override
    public <T, K> WireIn sequence(@NotNull T t, K kls, @NotNull TriConsumer<T, K, ValueIn> tReader) {
        assert defaultValue == null;
        tReader.accept(t, kls, this);
        return wireIn();
    }

    @Nullable
    @Override
    public <T> T applyToMarshallable(Function<WireIn, T> marshallableReader) {
        return (T) defaultValue;
    }

    @Nullable
    @Override
    public <T> T typedMarshallable() throws IORuntimeException {
        return (T) defaultValue;
    }

    @NotNull
    @Override
    public <T> ValueIn typePrefix(T t, @NotNull BiConsumer<T, CharSequence> ts) {
        ts.accept(t, null);
        return this;
    }

    @NotNull
    @Override
    public <T> WireIn typeLiteralAsText(T t, @NotNull BiConsumer<T, CharSequence> ts) throws IORuntimeException, BufferUnderflowException {
        ts.accept(t, null);
        return wireIn();
    }

    @Nullable
    @Override
    public Object marshallable(@NotNull Object object, SerializationStrategy strategy) throws BufferUnderflowException, IORuntimeException {
        return defaultValue;
    }

    @Override
    public <K extends ReadMarshallable, V extends ReadMarshallable> void typedMap(@NotNull Map<K, V> usingMap) {
        assert defaultValue == null;
        usingMap.clear();
    }

    @Nullable
    @Override
    public <K, V> Map<K, V> map(@NotNull Class<K> kClazz, @NotNull Class<V> vClass, @NotNull Map<K, V> usingMap) {
        assert defaultValue == null;
        usingMap.clear();
        return usingMap;
    }

    @Override
    public boolean bool() throws IORuntimeException {
        return defaultValue == Boolean.TRUE;
    }

    @Override
    public byte int8() {
        @Nullable Number o = (Number) defaultValue;
        if (o == null) o = 0;
        return o.byteValue();
    }

    @Override
    public short int16() {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        return o.shortValue();
    }

    @Override
    public int uint16() {
        @Nullable Number o = (Number) defaultValue;
        if (o == null) o = 0;
        return o.intValue();
    }

    @Override
    public int int32() {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        return o.intValue();
    }

    @Override
    public long int64() {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        return o.longValue();
    }

    @Override
    public double float64() {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        return o.doubleValue();
    }

    @Override
    public float float32() {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        return o.floatValue();
    }

    @Nullable
    @Override
    public <T> Class<T> typeLiteral() throws IORuntimeException, BufferUnderflowException {
        return (Class<T>) defaultValue;
    }

    @NotNull
    @Override
    public BracketType getBracketType() {
        return BracketType.NONE;
    }

    @Override
    public boolean isNull() {
        return defaultValue == null;
    }

    @Override
    public Object objectWithInferredType(Object using, SerializationStrategy strategy, Class type) {
        return defaultValue;
    }

    @Override
    public boolean isTyped() {
        return false;
    }

    @Override
    public Class typePrefix() {
        @Nullable Object o = defaultValue;
        if (o == null) return void.class;
        return o.getClass();
    }

    @Override
    public void resetState() {
    }
}
