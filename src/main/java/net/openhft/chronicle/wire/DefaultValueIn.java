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

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.bytes.internal.NoBytesStore;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.util.*;
import net.openhft.chronicle.core.values.BooleanValue;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.nio.BufferUnderflowException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.*;
/**
 * An implementation of {@link ValueIn} used when a requested field is not
 * present on the wire. It returns either {@code null}, a primitive zero or the
 * value configured by the {@link WireMarshaller}. This allows optional fields
 * to be skipped without breaking deserialisation.
 */
@SuppressWarnings("rawtypes")
public class DefaultValueIn implements ValueIn {

    /** The parent {@link WireIn} this instance belongs to. */
    private final WireIn wireIn;

    /** Value returned by the read methods when no field is present. */
    Object defaultValue;

    /**
     * Creates a {@code DefaultValueIn} bound to the supplied wire.
     */
    DefaultValueIn(WireIn wireIn) {
        this.wireIn = wireIn;
    }

    /**
     * Returns {@link #defaultValue} as a string.
     */
    @Nullable
    @Override
    public String text() {
        @Nullable Object o = defaultValue;
        return o == null ? null : o.toString();
    }

    /**
     * Appends the string form of {@link #defaultValue} to {@code sb}.
     * Returns {@code null} if no value is configured.
     */
    @Nullable
    @Override
    public StringBuilder textTo(@NotNull StringBuilder sb) {
        @Nullable Object o = defaultValue;
        if (o == null)
            return null;
        sb.append(o);
        return sb;
    }

    /**
     * Writes the string form of {@link #defaultValue} to the supplied bytes.
     * Returns {@code null} when the value is absent.
     */
    @Nullable
    @Override
    public Bytes<?> textTo(@NotNull Bytes<?> bytes) {
        @Nullable Object o = defaultValue;
        if (o == null)
            return null;
        bytes.write((BytesStore) o);
        return bytes;
    }

    /**
     * Writes {@link #defaultValue} to {@code toBytes} and returns the parent
     * wire.
     */
    @NotNull
    @Override
    public WireIn bytes(@NotNull BytesOut<?> toBytes) {
        @Nullable Object o = defaultValue;
        if (o == null)
            return wireIn();
        @NotNull BytesStore<?, ?> bytes = (BytesStore) o;
        toBytes.write(bytes);
        return wireIn();
    }

    /**
     * Copies {@link #defaultValue} into a {@link PointerBytesStore}. If absent
     * an empty store is set.
     */
    @Nullable
    @Override
    public WireIn bytesSet(@NotNull PointerBytesStore toBytes) {
        @Nullable Object o = defaultValue;
        if (o == null) {
            toBytes.set(NoBytesStore.NO_PAGE, 0);
            return wireIn();
        }
        @NotNull BytesStore<?, ?> bytes = (BytesStore) o;
        toBytes.set(bytes.addressForRead(0), bytes.realCapacity());
        return wireIn();
    }

    /**
     * Compares {@link #defaultValue} with {@code compareBytes} and passes the
     * result to {@code consumer}.
     */
    @NotNull
    @Override
    public WireIn bytesMatch(@NotNull BytesStore<?, ?> compareBytes, @NotNull BooleanConsumer consumer) {
        @Nullable Object o = defaultValue;
        @NotNull BytesStore<?, ?> bytes = (BytesStore) o;
        consumer.accept(compareBytes.contentEquals(bytes));
        return wireIn();
    }

    /**
     * Supplies {@link #defaultValue} to a {@link ReadBytesMarshallable}.
     */
    @NotNull
    @Override
    public WireIn bytes(@NotNull ReadBytesMarshallable wireInConsumer) {
        @Nullable Object o = defaultValue;
        if (o == null) {
            wireInConsumer.readMarshallable(Wires.NO_BYTES);
            return wireIn();
        }
        @Nullable BytesStore<?, ?> bytes = (BytesStore) o;
        wireInConsumer.readMarshallable(bytes.bytesForRead());
        return wireIn();
    }

    /**
     * Returns the backing byte array.
     */
    @Override
    public byte @NotNull [] bytes(byte[] using) {
        return (byte[]) defaultValue;
    }

    /**
     * Returns the parent {@link WireIn}.
     */
    @NotNull
    @Override
    public WireIn wireIn() {
        return wireIn;
    }

    /**
     * Always returns {@code 0} as no bytes are consumed.
     */
    @Override
    public long readLength() {
        return 0;
    }

    /**
     * No value is read so the parent wire is returned.
     */
    @NotNull
    @Override
    public WireIn skipValue() {
        return wireIn();
    }

    /**
     * Passes {@link #defaultValue} to {@code tFlag} and returns the parent wire.
     */
    @NotNull
    @Override
    public <T> WireIn bool(T t, @NotNull ObjBooleanConsumer<T> tFlag) {
        @Nullable Boolean o = (Boolean) defaultValue;
        tFlag.accept(t, o);
        return wireIn();
    }

    /**
     * Supplies the byte value of {@link #defaultValue} to {@code tb}.
     */
    @NotNull
    @Override
    public <T> WireIn int8(@NotNull T t, @NotNull ObjByteConsumer<T> tb) {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        tb.accept(t, o.byteValue());
        return wireIn();
    }

    /**
     * Supplies the unsigned byte value of {@link #defaultValue}.
     */
    @NotNull
    @Override
    public <T> WireIn uint8(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        ti.accept(t, o.shortValue());
        return wireIn();
    }

    /**
     * Supplies the short value of {@link #defaultValue}.
     */
    @NotNull
    @Override
    public <T> WireIn int16(@NotNull T t, @NotNull ObjShortConsumer<T> ti) {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        ti.accept(t, o.shortValue());
        return wireIn();
    }

    /**
     * Supplies the unsigned short value of {@link #defaultValue}.
     */
    @NotNull
    @Override
    public <T> WireIn uint16(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        ti.accept(t, o.intValue());
        return wireIn();
    }

    /**
     * Supplies the int value of {@link #defaultValue}.
     */
    @NotNull
    @Override
    public <T> WireIn int32(@NotNull T t, @NotNull ObjIntConsumer<T> ti) {
        @Nullable Number o = (Number) defaultValue;
        if (o == null) o = 0;
        ti.accept(t, o.intValue());
        return wireIn();
    }

    /**
     * Supplies the unsigned int value of {@link #defaultValue}.
     */
    @NotNull
    @Override
    public <T> WireIn uint32(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        tl.accept(t, o.longValue());
        return wireIn();
    }

    /**
     * Supplies the long value of {@link #defaultValue}.
     */
    @NotNull
    @Override
    public <T> WireIn int64(@NotNull T t, @NotNull ObjLongConsumer<T> tl) {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        tl.accept(t, o.longValue());
        return wireIn();
    }

    /**
     * Supplies the float value of {@link #defaultValue}.
     */
    @NotNull
    @Override
    public <T> WireIn float32(@NotNull T t, @NotNull ObjFloatConsumer<T> tf) {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        tf.accept(t, o.floatValue());
        return wireIn();
    }

    /**
     * Supplies the double value of {@link #defaultValue}.
     */
    @NotNull
    @Override
    public <T> WireIn float64(@NotNull T t, @NotNull ObjDoubleConsumer<T> td) {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        td.accept(t, o.doubleValue());
        return wireIn();
    }

    /**
     * Passes the {@link LocalTime} held in {@link #defaultValue}.
     */
    @NotNull
    @Override
    public <T> WireIn time(@NotNull T t, @NotNull BiConsumer<T, LocalTime> setLocalTime) {
        @Nullable LocalTime o = (LocalTime) defaultValue;
        setLocalTime.accept(t, o);
        return wireIn();
    }

    /**
     * Passes the {@link ZonedDateTime} held in {@link #defaultValue}.
     */
    @NotNull
    @Override
    public <T> WireIn zonedDateTime(@NotNull T t, @NotNull BiConsumer<T, ZonedDateTime> tZonedDateTime) {
        @Nullable ZonedDateTime o = (ZonedDateTime) defaultValue;
        tZonedDateTime.accept(t, o);
        return wireIn();
    }

    /**
     * Passes the {@link LocalDate} held in {@link #defaultValue}.
     */
    @NotNull
    @Override
    public <T> WireIn date(@NotNull T t, @NotNull BiConsumer<T, LocalDate> tLocalDate) {
        @NotNull LocalDate o = (LocalDate) defaultValue;
        tLocalDate.accept(t, o);
        return wireIn();
    }

    /**
     * Always {@code false} as no sequence exists.
     */
    @Override
    public boolean hasNext() {
        return false;
    }

    /**
     * Always {@code false} as no sequence exists.
     */
    @Override
    public boolean hasNextSequenceItem() {
        return false;
    }

    /**
     * Supplies the {@link UUID} from {@link #defaultValue}.
     */
    @NotNull
    @Override
    public <T> WireIn uuid(@NotNull T t, @NotNull BiConsumer<T, UUID> tuuid) {
        @NotNull UUID o = (UUID) defaultValue;
        tuuid.accept(t, o);
        return wireIn();
    }

    /**
     * Not implemented for default values.
     */
    @NotNull
    @Override
    public <T> WireIn int64array(@Nullable LongArrayValues values, T t, @NotNull BiConsumer<T, LongArrayValues> setter) {
        throw new UnsupportedOperationException("todo");
    }

    /**
     * Sets {@code value} from {@link #defaultValue}.
     */
    @NotNull
    @Override
    public WireIn int64(@NotNull LongValue value) {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        value.setValue(o.longValue());
        return wireIn();
    }

    /**
     * Sets {@code value} from {@link #defaultValue}.
     */
    @NotNull
    @Override
    public WireIn int32(@NotNull IntValue value) {
        @Nullable Number o = (Number) defaultValue;
        if (o == null) o = 0;
        value.setValue(o.intValue());
        return wireIn();
    }

    /**
     * Not implemented for default values.
     */
    @Override
    public WireIn bool(@NotNull final BooleanValue ret) {
        throw new UnsupportedOperationException("todo");
    }

    /**
     * Provides {@link #defaultValue} to {@code setter}.
     */
    @NotNull
    @Override
    public <T> WireIn int64(@Nullable LongValue value, T t, @NotNull BiConsumer<T, LongValue> setter) {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        value.setValue(o.longValue());
        setter.accept(t, value);
        return wireIn();
    }

    /**
     * Provides {@link #defaultValue} to {@code setter}.
     */
    @NotNull
    @Override
    public <T> WireIn int32(@Nullable IntValue value, T t, @NotNull BiConsumer<T, IntValue> setter) {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        value.setValue(o.intValue());
        setter.accept(t, value);
        return wireIn();
    }

    /**
     * Always returns {@code false} as there is no sequence.
     */
    @Override
    public <T> boolean sequence(@NotNull T t, @NotNull BiConsumer<T, ValueIn> tReader) {
        return false;
    }

    /**
     * Always returns {@code false}; there is no data to read.
     */
    @Override
    public <T> boolean sequence(List<T> list, @NotNull List<T> buffer, Supplier<T> bufferAdd, Reader reader0) {
        return false;
    }

    /**
     * Invokes {@code tReader} with {@code kls} and this instance.
     */
    @NotNull
    @Override
    public <T, K> WireIn sequence(@NotNull T t, K kls, @NotNull TriConsumer<T, K, ValueIn> tReader) throws InvalidMarshallableException {
        assert defaultValue == null;
        tReader.accept(t, kls, this);
        return wireIn();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T applyToMarshallable(Function<WireIn, T> marshallableReader) {
        return (T) defaultValue;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> T typedMarshallable() throws IORuntimeException {
        return (T) defaultValue;
    }

    /**
     * Supplies a {@code null} type prefix.
     */
    @NotNull
    @Override
    public <T> ValueIn typePrefix(T t, @NotNull BiConsumer<T, CharSequence> ts) {
        ts.accept(t, null);
        return this;
    }

    /**
     * Supplies a {@code null} type prefix to {@code ts} and returns the wire.
     */
    @NotNull
    @Override
    public <T> WireIn typeLiteralAsText(T t, @NotNull BiConsumer<T, CharSequence> ts) throws IORuntimeException, BufferUnderflowException {
        ts.accept(t, null);
        return wireIn();
    }

    /**
     * Delegates to the parent wire.
     */
    @Override
    public ClassLookup classLookup() {
        return wireIn.classLookup();
    }

    /**
     * Returns {@link #defaultValue} unchanged.
     */
    @Nullable
    @Override
    public Object marshallable(@NotNull Object object, @NotNull SerializationStrategy strategy) throws BufferUnderflowException, IORuntimeException {
        return defaultValue;
    }

    /**
     * Returns {@code true} if {@link #defaultValue} is {@code Boolean.TRUE}.
     */
    @Override
    public boolean bool() throws IORuntimeException {
        return defaultValue == Boolean.TRUE;
    }

    /**
     * Returns the byte value of {@link #defaultValue}.
     */
    @Override
    public byte int8() {
        @Nullable Number o = (Number) defaultValue;
        if (o == null) o = 0;
        return o.byteValue();
    }

    /**
     * Returns the short value of {@link #defaultValue}.
     */
    @Override
    public short int16() {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        return o.shortValue();
    }

    /**
     * Returns the unsigned short value of {@link #defaultValue}.
     */
    @Override
    public int uint16() {
        @Nullable Number o = (Number) defaultValue;
        if (o == null) o = 0;
        return o.intValue();
    }

    /**
     * Returns the int value of {@link #defaultValue}.
     */
    @Override
    public int int32() {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        return o.intValue();
    }

    /**
     * Returns the long value of {@link #defaultValue}.
     */
    @Override
    public long int64() {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        return o.longValue();
    }

    /**
     * Returns the double value of {@link #defaultValue}.
     */
    @Override
    public double float64() {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        return o.doubleValue();
    }

    /**
     * Returns the float value of {@link #defaultValue}.
     */
    @Override
    public float float32() {
        @NotNull Number o = (Number) defaultValue;
        if (o == null) o = 0;
        return o.floatValue();
    }

    /**
     * Returns {@link #defaultValue} as a type literal.
     */
    @Override
    public Type typeLiteral(BiFunction<CharSequence, ClassNotFoundException, Type> unresolvedHandler) {
        return (Type) defaultValue;
    }

    /**
     * Always {@link BracketType#NONE} as no value is read.
     */
    @NotNull
    @Override
    public BracketType getBracketType() {
        return BracketType.NONE;
    }

    /**
     * Returns {@code true} when {@link #defaultValue} is {@code null}.
     */
    @Override
    public boolean isNull() {
        return defaultValue == null;
    }

    /**
     * Returns {@link #defaultValue}.
     */
    @Override
    public Object objectWithInferredType(Object using, SerializationStrategy strategy, Class<?> type) {
        return defaultValue;
    }

    /**
     * Always {@code false}; nothing was found on the wire.
     */
    @Override
    public boolean isPresent() {
        return false;
    }

    /**
     * Always {@code false}.
     */
    @Override
    public boolean isTyped() {
        return false;
    }

    /**
     * Returns the class of {@link #defaultValue} or {@code void.class}.
     */
    @Override
    public Class<?> typePrefix() {
        @Nullable Object o = defaultValue;
        if (o == null) return void.class;
        return o.getClass();
    }

    /**
     * Nothing to reset.
     */
    @Override
    public void resetState() {
        // Do nothing
    }
}
