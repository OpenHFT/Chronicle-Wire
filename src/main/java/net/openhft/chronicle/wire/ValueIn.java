/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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
import net.openhft.chronicle.core.UnresolvedType;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.Resettable;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.util.*;
import net.openhft.chronicle.core.values.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.*;

/**
 * Read in data after reading a field.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface ValueIn {
    Consumer<ValueIn> DISCARD = v -> {
    };

    /*
     * Text / Strings.
     */
    @NotNull
    default <T> WireIn text(T t, @NotNull BiConsumer<T, String> ts) {
        @Nullable final String text = text();
        ts.accept(t, text);
        return wireIn();
    }

    @NotNull
    default WireIn text(@NotNull StringBuilder sb) {
        if (textTo(sb) == null)
            sb.setLength(0);
        return wireIn();
    }

    default char character() {
        @Nullable CharSequence cs = textTo(WireInternal.acquireStringBuilderForValueIn());
        if (cs == null || cs.length() == 0)
            return '\u0000';

        return cs.charAt(0);
    }

    @NotNull
    default WireIn text(@NotNull Bytes<?> sdo) {
        sdo.clear();
        textTo(sdo);
        return wireIn();
    }

    @Nullable
    String text();

    @Nullable
    StringBuilder textTo(@NotNull StringBuilder sb);

    @Nullable
    Bytes<?> textTo(@NotNull Bytes<?> bytes);

    @NotNull
    WireIn bytes(@NotNull BytesOut<?> toBytes);

    default WireIn bytes(@NotNull BytesOut<?> toBytes, boolean clearBytes) {
        if (clearBytes)
            toBytes.clear();
        return bytes(toBytes);
    }

    @NotNull
    default WireIn bytesLiteral(@NotNull BytesOut<?> toBytes) {
        return bytes(toBytes);
    }

    @Nullable
    default BytesStore bytesLiteral() {
        return bytesStore();
    }

    @Nullable
    WireIn bytesSet(@NotNull PointerBytesStore toBytes);

    @NotNull
    WireIn bytesMatch(@NotNull BytesStore compareBytes, BooleanConsumer consumer);

    @NotNull
    WireIn bytes(@NotNull ReadBytesMarshallable bytesMarshallable);

    default byte @Nullable [] bytes() {
        return bytes((byte[]) null);
    }

    byte @Nullable [] bytes(byte[] using);

    @Nullable
    default BytesStore bytesStore() {
        byte @Nullable [] bytes = bytes();
        return bytes == null ? null : BytesStore.wrap(bytes);
    }

    default void byteBuffer(@NotNull ByteBuffer bb) {
        bb.put(bytes());
    }

    @NotNull
    WireIn wireIn();

    /**
     * the length of the field as bytes including any encoding and header character
     */
    long readLength();

    @NotNull
    WireIn skipValue();

    @NotNull <T> WireIn bool(T t, @NotNull ObjBooleanConsumer<T> tFlag);

    @NotNull <T> WireIn int8(@NotNull T t, @NotNull ObjByteConsumer<T> tb);

    @NotNull <T> WireIn uint8(@NotNull T t, @NotNull ObjShortConsumer<T> ti);

    @NotNull <T> WireIn int16(@NotNull T t, @NotNull ObjShortConsumer<T> ti);

    @NotNull <T> WireIn uint16(@NotNull T t, @NotNull ObjIntConsumer<T> ti);

    @NotNull <T> WireIn int32(@NotNull T t, @NotNull ObjIntConsumer<T> ti);

    @NotNull <T> WireIn uint32(@NotNull T t, @NotNull ObjLongConsumer<T> tl);

    @NotNull <T> WireIn int64(@NotNull T t, @NotNull ObjLongConsumer<T> tl);

    @NotNull <T> WireIn float32(@NotNull T t, @NotNull ObjFloatConsumer<T> tf);

    @NotNull <T> WireIn float64(@NotNull T t, @NotNull ObjDoubleConsumer<T> td);

    @NotNull <T> WireIn time(@NotNull T t, @NotNull BiConsumer<T, LocalTime> setLocalTime);

    @NotNull <T> WireIn zonedDateTime(@NotNull T t, @NotNull BiConsumer<T, ZonedDateTime> tZonedDateTime);

    @NotNull <T> WireIn date(@NotNull T t, @NotNull BiConsumer<T, LocalDate> tLocalDate);

    default LocalDate date() {
        return WireInternal.intern(LocalDate.class, text());
    }

    default LocalTime time() {
        return WireInternal.intern(LocalTime.class, text());
    }

    default LocalDateTime dateTime() {
        return WireInternal.intern(LocalDateTime.class, text());
    }

    default ZonedDateTime zonedDateTime() {
        return WireInternal.intern(ZonedDateTime.class, text());
    }

    boolean hasNext();

    boolean hasNextSequenceItem();

    @NotNull <T> WireIn uuid(@NotNull T t, @NotNull BiConsumer<T, UUID> tuuid);

    @NotNull <T> WireIn int64array(@Nullable LongArrayValues values, T t, @NotNull BiConsumer<T, LongArrayValues> setter);

    @NotNull
    default WireIn int128(@NotNull TwoLongValue value) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    WireIn int64(@NotNull LongValue value);

    @NotNull
    WireIn int32(@NotNull IntValue value);

    @NotNull
    default LongValue int64ForBinding(@Nullable LongValue value) {
        @NotNull LongValue ret = value == null ? wireIn().newLongReference() : value;
        int64(ret);
        return ret;
    }

    WireIn bool(@NotNull BooleanValue ret);

    @NotNull <T> WireIn int64(@Nullable LongValue value, T t, @NotNull BiConsumer<T, LongValue> setter);

    @NotNull <T> WireIn int32(@Nullable IntValue value, T t, @NotNull BiConsumer<T, IntValue> setter);

    <T> boolean sequence(@NotNull T t, @NotNull BiConsumer<T, ValueIn> tReader);

    <T> boolean sequence(List<T> list, @NotNull List<T> buffer, Supplier<T> bufferAdd, Reader reader0);

    default <T> boolean sequence(@NotNull T t, @NotNull SerializationStrategy<T> tReader) {
        return sequence(t, (using, in) -> tReader.readUsing(null, using, in, BracketType.UNKNOWN));
    }

    default <T> void reader0(ValueIn v, List<T> list, List<T> buffer, Supplier<T> bufferAdd) {
        while (v.hasNextSequenceItem()) {
            int size = list.size();
            if (buffer.size() <= size) {
                buffer.add(bufferAdd.get());
            }

            final Object t = buffer.get(size);
            if (t instanceof Resettable) ((Resettable) t).reset();
            list.add((T) v.object(t, t.getClass()));
        }
    }

    /**
     * sequence to use when using a cached buffer
     *
     * @param list      of items to populate
     * @param buffer    of objects of the same type to reuse
     * @param bufferAdd supplier to call when the buffer needs extending
     * @return true if there is any data.
     */
    default <T> boolean sequence(@NotNull List<T> list,
                                 @NotNull List<T> buffer,
                                 @NotNull Supplier<T> bufferAdd) {
        list.clear();
        return sequence(list, buffer, bufferAdd, this::reader0);
    }

    @NotNull <T, K> WireIn sequence(@NotNull T t, K k, @NotNull TriConsumer<T, K, ValueIn> tReader);

    default <T> int sequenceWithLength(@NotNull T t, @NotNull ToIntBiFunction<ValueIn, T> tReader) {
        int[] length = {0};
        sequence(t, (tt, in) -> length[0] = tReader.applyAsInt(in, tt));
        return length[0];
    }

    default int array(Bytes[] array) {
        return sequenceWithLength(array, (in, a) -> {
            int i = 0;
            while (in.hasNextSequenceItem() && i < a.length) {
                if (a[i] == null)
                    a[i] = Bytes.allocateElasticOnHeap(32);
                bytes(a[i++]);
            }
            return i;
        });
    }

    default int array(double[] array) {
        return sequenceWithLength(array, (in, a) -> {
            int i = 0;
            while (in.hasNextSequenceItem() && i < a.length)
                a[i++] = in.float64();
            return i;
        });
    }

    default int arrayDelta(double[] array) {
        return sequenceWithLength(array, (in, a) -> {
            if (!in.hasNextSequenceItem() || a.length == 0)
                return 0;
            double a0 = a[0] = in.float64();
            int i = 1;
            while (in.hasNextSequenceItem() && i < a.length)
                a[i++] = in.float64() + a0;
            return i;
        });
    }

    default int array(boolean[] array) {
        return sequenceWithLength(array, (in, a) -> {
            int i = 0;
            while (in.hasNextSequenceItem() && i < a.length)
                a[i++] = in.bool();
            return i;
        });
    }

    default int array(long[] array) {
        return sequenceWithLength(array, (in, a) -> {
            int i = 0;
            while (in.hasNextSequenceItem() && i < a.length)
                a[i++] = in.int64();
            return i;
        });
    }

    default int arrayDelta(long[] array) {
        return sequenceWithLength(array, (in, a) -> {
            if (!in.hasNextSequenceItem() || a.length == 0)
                return 0;
            long a0 = a[0] = in.int64();
            int i = 1;
            while (in.hasNextSequenceItem() && i < a.length)
                a[i++] = in.int64() + a0;
            return i;
        });
    }

    default int array(int[] array) {
        return sequenceWithLength(array, (in, a) -> {
            int i = 0;
            while (in.hasNextSequenceItem() && i < a.length)
                a[i++] = in.int32();
            return i;
        });
    }

    default int array(byte[] array) {
        return sequenceWithLength(array, (in, a) -> {
            int i = 0;
            while (in.hasNextSequenceItem() && i < a.length)
                a[i++] = in.int8();
            return i;
        });
    }

    default <T> Set<T> set(Class<T> t) {
        return collection(LinkedHashSet::new, t);
    }

    default <T> List<T> list(Class<T> t) {
        return collection(ArrayList::new, t);
    }

    default <T, C extends Collection<T>> C collection(@NotNull Supplier<C> supplier, Class<T> t) {
        C list = supplier.get();
        sequence(list, t, (s, kls, v) -> {
            while (v.hasNextSequenceItem())
                s.add(v.object(kls));
        });
        return list;
    }

    @NotNull
    default <O, T extends ReadMarshallable> WireIn set(@NotNull O o, Function<O, T> tSupplier) {
        return collection(o, tSupplier);
    }

    @NotNull
    default <O, T extends ReadMarshallable> WireIn list(@NotNull O o, Function<O, T> tSupplier) {
        return collection(o, tSupplier);
    }

    @NotNull
    default <O, T extends ReadMarshallable> WireIn collection(@NotNull O o, Function<O, T> tSupplier) {
        sequence(o, tSupplier, (o2, ts, v) -> {
            while (v.hasNextSequenceItem()) {
                T t = ts.apply(o2);
                v.marshallable(t);
            }
        });
        return wireIn();
    }

    @Nullable
    default <K, V> Map<K, V> marshallableAsMap(Class<K> kClass, @NotNull Class<V> vClass) {
        return marshallableAsMap(kClass, vClass, new LinkedHashMap<>());
    }

    @Nullable
    default <K, V> Map<K, V> marshallableAsMap(Class<K> kClass, @NotNull Class<V> vClass, @NotNull Map<K, V> map) {
        return marshallable(m -> m.readAllAsMap(kClass, vClass, map)) ? map : null;
    }

    @Nullable <T> T applyToMarshallable(Function<WireIn, T> marshallableReader);

    @Nullable <T> T typedMarshallable() throws IORuntimeException;

    @Nullable
    default <T> T typedMarshallable(@NotNull Function<Class, ReadMarshallable> marshallableFunction)
            throws IORuntimeException {
        @Nullable final Class aClass = typePrefix();

        if (ReadMarshallable.class.isAssignableFrom(aClass)) {
            final ReadMarshallable marshallable = marshallableFunction.apply(aClass);
            marshallable(marshallable);
            return (T) marshallable;
        }
        return (T) object(null, aClass);
    }

    @NotNull <T> ValueIn typePrefix(T t, @NotNull BiConsumer<T, CharSequence> ts);

    @NotNull <T> WireIn typeLiteralAsText(T t, @NotNull BiConsumer<T, CharSequence> classNameConsumer)
            throws IORuntimeException, BufferUnderflowException;

    ClassLookup classLookup();

    @NotNull
    default <T> WireIn typeLiteral(T t, @NotNull BiConsumer<T, Class> classConsumer) throws IORuntimeException {
        return typeLiteralAsText(t, (o, x) ->
                classConsumer.accept(o, classLookup().forName(x))
        );
    }

    @NotNull
    default <T> WireIn typeLiteral(T t, @NotNull BiConsumer<T, Class> classConsumer, Class defaultClass) {
        return typeLiteralAsText(t, (o, x) -> {
            Class u = classLookup().forName(x);
            classConsumer.accept(o, u);
        });
    }

    @Nullable
    Object marshallable(@NotNull Object object, @NotNull SerializationStrategy strategy)
            throws BufferUnderflowException, IORuntimeException;

    default boolean marshallable(@NotNull Serializable object) throws BufferUnderflowException, IORuntimeException {
        return marshallable(object, SerializationStrategies.SERIALIZABLE) != null;
    }

    default boolean marshallable(@NotNull ReadMarshallable object) throws BufferUnderflowException, IORuntimeException {
        return marshallable(object, SerializationStrategies.MARSHALLABLE) != null;
    }

    boolean bool();

    byte int8();

    short int16();

    int uint16();

    int int32();

    default int int32(int previous) {
        return int32();
    }

    long int64();

    default long int64(long previous) {
        return int64();
    }

    float float32();

    default float float32(float previous) {
        return float32();
    }

    /**
     *
     * @return the value as a float, or -0.0 indicates that we have not been able to parse this data ( we don't throw an exception )
     */
    double float64();

    default double float64(double previous) {
        return float64();
    }

    @Nullable
    default <T> Class<T> typeLiteral() throws IORuntimeException, BufferUnderflowException {
        return (Class<T>) typeLiteral((sb, e) -> {
            throw new IORuntimeException(e);
        });
    }

    @Nullable
    default Type lenientTypeLiteral() throws IORuntimeException, BufferUnderflowException {
        return typeLiteral((sb, e) -> UnresolvedType.of(sb.toString()));
    }

    Type typeLiteral(BiFunction<CharSequence, ClassNotFoundException, Type> unresolvedHandler);

    default Throwable throwable(boolean appendCurrentStack) {
        return WireInternal.throwable(this, appendCurrentStack);
    }

    @Nullable
    default <E extends Enum<E>> E asEnum(Class<E> eClass) {
        StringBuilder sb = WireInternal.acquireStringBuilderForValueIn();
        text(sb);
        return sb.length() == 0 ? null : WireInternal.internEnum(eClass, sb);
    }

    @NotNull
    default <E extends Enum<E>> WireIn asEnum(Class<E> eClass, @NotNull Consumer<E> eConsumer) {
        eConsumer.accept(asEnum(eClass));
        return wireIn();
    }

    @NotNull
    default <E extends Enum<E>, T> WireIn asEnum(Class<E> eClass, T t, @NotNull BiConsumer<T, E> teConsumer) {
        teConsumer.accept(t, asEnum(eClass));
        return wireIn();
    }

    /**
     * @param clazz Expected object type. <code>null</code> can be passed to request proxy marshallable tuple
     *              if {@link Wires#GENERATE_TUPLES} is enabled.
     */
    @Nullable
    default <E> E object(@Nullable Class<E> clazz) {
        return Wires.object0(this, null, clazz);
    }

    @Nullable
    default Object object() {
        @Nullable final Object o = objectWithInferredType(null, SerializationStrategies.ANY_OBJECT, null);
        return o;
    }

    @Nullable
    default <E> E object(@Nullable E using, @Nullable Class clazz) {
        return Wires.object0(this, using, clazz);
    }

    @NotNull
    BracketType getBracketType();

    boolean isNull();

    @Nullable
    default <T, E> WireIn object(@NotNull Class<E> clazz, T t, @NotNull BiConsumer<T, E> e) {
        e.accept(t, object(clazz));
        return wireIn();
    }

    boolean isTyped();

    @Nullable
    Class typePrefix();

    @Nullable
    default Object typePrefixOrObject(Class tClass) {
        return typePrefix();
    }

    void resetState();

    @Nullable
    Object objectWithInferredType(Object using, SerializationStrategy strategy, Class type);

    /**
     * Check if the value is present
     *
     * @return true if the value was present in the Wire, false otherwise
     */
    default boolean isPresent() {
        return true;
    }

    @NotNull
    default UUID uuid() {
        return UUID.fromString(text());
    }

    default boolean isBinary() {
        return false;
    }

    default long readLong(LongConverter longConverter) {
        StringBuilder sb = WireInternal.acquireStringBuilderForValueIn();
        text(sb);
        return longConverter.parse(sb);
    }

    default boolean readBoolean() {
        return bool();
    }

    default byte readByte() {
        return int8();
    }

    default char readChar() {
        return (char) uint16();
    }

    default short readShort() {
        return int16();
    }

    default int readInt() {
        return int32();
    }

    default long readLong() {
        return int64();
    }

    default float readFloat() {
        return float32();
    }

    default double readDouble() {
        return float64();
    }

    default String readString() {
        return text();
    }

    interface Reader {
        <T> void accept(ValueIn valueIn, List<T> list, List<T> buffer, Supplier<T> bufferAdd);
    }
}
