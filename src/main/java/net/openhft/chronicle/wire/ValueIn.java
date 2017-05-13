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
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.*;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.*;

import static net.openhft.chronicle.core.util.ReadResolvable.readResolve;

/**
 * Read in data after reading a field.
 */
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
        @Nullable CharSequence cs = textTo(Wires.acquireBytes());
        if (cs == null || cs.length() == 0)
            return '\u0000';

        return cs.charAt(0);
    }


    @NotNull
    default WireIn text(@NotNull Bytes sdo) {
        sdo.clear();
        textTo(sdo);
        return wireIn();
    }

    @Nullable
    String text();

    @Nullable
    StringBuilder textTo(@NotNull StringBuilder sb);

    @Nullable
    Bytes textTo(@NotNull Bytes bytes);

    @NotNull
    @Deprecated
    default WireIn bytes(@NotNull Bytes toBytes) {
        return bytes((BytesOut) toBytes);
    }

    @NotNull
    WireIn bytes(@NotNull BytesOut toBytes);


    default WireIn bytes(@NotNull BytesOut toBytes, boolean clearBytes) {
        if (clearBytes)
            toBytes.clear();
        return bytes(toBytes);
    }

    @NotNull
    default WireIn bytesLiteral(@NotNull BytesOut toBytes) {
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

    @Nullable
    byte[] bytes();

    @Nullable
    default BytesStore bytesStore() {
        @Nullable byte[] bytes = bytes();
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

    WireIn skipValue();

    @NotNull
    <T> WireIn bool(T t, @NotNull ObjBooleanConsumer<T> tFlag);

    @NotNull
    <T> WireIn int8(@NotNull T t, @NotNull ObjByteConsumer<T> tb);

    @NotNull
    <T> WireIn uint8(@NotNull T t, @NotNull ObjShortConsumer<T> ti);

    @NotNull
    <T> WireIn int16(@NotNull T t, @NotNull ObjShortConsumer<T> ti);

    @NotNull
    <T> WireIn uint16(@NotNull T t, @NotNull ObjIntConsumer<T> ti);

    @NotNull
    <T> WireIn int32(@NotNull T t, @NotNull ObjIntConsumer<T> ti);

    @NotNull
    <T> WireIn uint32(@NotNull T t, @NotNull ObjLongConsumer<T> tl);

    @NotNull
    <T> WireIn int64(@NotNull T t, @NotNull ObjLongConsumer<T> tl);

    @NotNull
    <T> WireIn float32(@NotNull T t, @NotNull ObjFloatConsumer<T> tf);

    @NotNull
    <T> WireIn float64(@NotNull T t, @NotNull ObjDoubleConsumer<T> td);

    @NotNull
    <T> WireIn time(@NotNull T t, @NotNull BiConsumer<T, LocalTime> setLocalTime);

    @NotNull
    <T> WireIn zonedDateTime(@NotNull T t, @NotNull BiConsumer<T, ZonedDateTime> tZonedDateTime);

    @NotNull
    <T> WireIn date(@NotNull T t, @NotNull BiConsumer<T, LocalDate> tLocalDate);

    default LocalDate date() {
        return LocalDate.parse(text());
    }

    default LocalTime time() {
        return LocalTime.parse(text());
    }

    default LocalDateTime dateTime() {
        return LocalDateTime.parse(text());
    }

    default ZonedDateTime zonedDateTime() {
        return ZonedDateTime.parse(text());
    }

    boolean hasNext();

    boolean hasNextSequenceItem();

    @NotNull
    <T> WireIn uuid(@NotNull T t, @NotNull BiConsumer<T, UUID> tuuid);

    @NotNull
    <T> WireIn int64array(@Nullable LongArrayValues values, T t, @NotNull BiConsumer<T, LongArrayValues> setter);

    @NotNull
    WireIn int64(@NotNull LongValue value);

    @NotNull
    WireIn int32(@NotNull IntValue value);

    @NotNull
    default LongValue int64ForBinding(@Nullable LongValue value) {
        @NotNull LongValue ret = wireIn().newLongReference();
        int64(ret);
        return ret;
    }

    @NotNull
    default IntValue int32ForBinding(@Nullable LongValue value) {
        @NotNull IntValue ret = wireIn().newIntReference();
        int32(ret);
        return ret;
    }

    @NotNull
    <T> WireIn int64(@Nullable LongValue value, T t, @NotNull BiConsumer<T, LongValue> setter);

    @NotNull
    <T> WireIn int32(@Nullable IntValue value, T t, @NotNull BiConsumer<T, IntValue> setter);

    @NotNull
    <T> boolean sequence(@NotNull T t, @NotNull BiConsumer<T, ValueIn> tReader);

    default <T> boolean sequence(@NotNull T t, @NotNull SerializationStrategy<T> tReader) {
        return sequence(t, tReader::readUsing);
    }

    @NotNull
    <T, K> WireIn sequence(@NotNull T t, K kls, @NotNull TriConsumer<T, K, ValueIn> tReader);

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
    default <O, T extends ReadMarshallable, C extends Collection<T>> WireIn collection(@NotNull O o, Function<O, T> tSupplier) {
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
        return marshallable(m -> {
            m.readAllAsMap(kClass, vClass, map);
        }) ? map : null;
    }

    <T> T applyToMarshallable(Function<WireIn, T> marshallableReader);

    @Nullable
    <T> T typedMarshallable() throws IORuntimeException;

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

    @NotNull
    <T> ValueIn typePrefix(T t, @NotNull BiConsumer<T, CharSequence> ts);

    @NotNull
    <T> WireIn typeLiteralAsText(T t, @NotNull BiConsumer<T, CharSequence> classNameConsumer)
            throws IORuntimeException, BufferUnderflowException;

    @NotNull
    default <T> WireIn typeLiteral(T t, @NotNull BiConsumer<T, Class> classConsumer) throws IORuntimeException {
        return typeLiteralAsText(t, (o, x) -> {
            try {
                classConsumer.accept(o, ClassAliasPool.CLASS_ALIASES.forName(x));
            } catch (ClassNotFoundException e) {
                throw new IORuntimeException(e);
            }
        });
    }

    @NotNull
    default <T> WireIn typeLiteral(T t, @NotNull BiConsumer<T, Class> classConsumer, Class defaultClass) {
        return typeLiteralAsText(t, (o, x) -> {
            Class u = defaultClass;
            try {
                u = ClassAliasPool.CLASS_ALIASES.forName(x);
            } catch (ClassNotFoundException e) {
                // use default class.
            }
            classConsumer.accept(o, u);
        });
    }

    @Nullable
    Object marshallable(Object object, SerializationStrategy strategy)
            throws BufferUnderflowException, IORuntimeException;

    default boolean marshallable(@NotNull Serializable object) throws BufferUnderflowException, IORuntimeException {
        return marshallable(object, SerializationStrategies.SERIALIZABLE) != null;
    }

    default boolean marshallable(@NotNull ReadMarshallable object) throws BufferUnderflowException, IORuntimeException {
        return marshallable(object, SerializationStrategies.MARSHALLABLE) != null;
    }

    /**
     * reads the map from the wire
     *
     * @deprecated use marshallableAsMap
     */
    @Deprecated
    default void map(@NotNull Map<String, String> usingMap) {
        map(String.class, String.class, usingMap);
    }

    /**
     * @deprecated use marshallableAsMap
     */
    @Deprecated
    <K extends ReadMarshallable, V extends ReadMarshallable>
    void typedMap(@NotNull final Map<K, V> usingMap);

    /**
     * reads the map from the wire
     *
     * @deprecated use marshallableAsMap
     */
    @Deprecated
    @Nullable
    <K, V> Map<K, V> map(@NotNull Class<K> kClazz,
                         @NotNull Class<V> vClass,
                         Map<K, V> usingMap);

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

    double float64();

    default double float64(double previous) {
        return float64();
    }

    <T> Class<T> typeLiteral() throws IORuntimeException, BufferUnderflowException;

    default Throwable throwable(boolean appendCurrentStack) {
        return WireInternal.throwable(this, appendCurrentStack);
    }

    @Nullable
    default <E extends Enum<E>> E asEnum(Class<E> eClass) {
        StringBuilder sb = WireInternal.acquireStringBuilder();
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

    @Nullable
    default <E> E object(@NotNull Class<E> clazz) {
        return object(null, clazz);
    }

    @Nullable
    default Object object() {
        @Nullable final Object o = objectWithInferredType(null, SerializationStrategies.ANY_OBJECT, null);
        return o;
    }

    @Nullable
    default <E> E object(@Nullable E using, @Nullable Class clazz) {
        @Nullable final Class clazz2 = typePrefix();
        if (clazz2 == void.class) {
            text();
            return null;
        }
        if (clazz2 != null && (clazz == null
                || clazz.isAssignableFrom(clazz2)
                || ReadResolvable.class.isAssignableFrom(clazz2)
                || !ObjectUtils.isConcreteClass(clazz))) {
            clazz = clazz2;
            if (!clazz.isInstance(using))
                using = null;
        }
        if (clazz == null)
            clazz = Object.class;
        SerializationStrategy<E> strategy = Wires.CLASS_STRATEGY.get(clazz);
        BracketType brackets = strategy.bracketType();
        if (brackets == BracketType.UNKNOWN)
            brackets = getBracketType();

        if (clazz != null && Date.class.isAssignableFrom(clazz)) {
            // skip the field if it is there.
            wireIn().read();
            final long time = int64();
            if (using instanceof Date) {
                ((Date) using).setTime(time);
                return using;
            } else
                return (E) new Date(time);
        }

        switch (brackets) {
            case MAP:
                if (clazz == Object.class)
                    strategy = SerializationStrategies.MAP;
                if (using == null)
                    using = (E) strategy.newInstance(clazz);
                if (Throwable.class.isAssignableFrom(clazz))
                    return (E) WireInternal.throwable(this, false, (Throwable) using);

                if (using == null)
                    Jvm.warn().on(ValueIn.class, "failed to create instance of clazz=" + clazz);

                @Nullable Object ret = marshallable(using, strategy);
                return readResolve(ret);

            case SEQ:
                if (clazz == Object.class)
                    strategy = SerializationStrategies.LIST;
                if (using == null)
                    using = (E) strategy.newInstance(clazz);

                return sequence(using, strategy::readUsing) ? readResolve(using) : null;

            case NONE:
                @NotNull final E e = (E) strategy.readUsing(using, this);
                return (E) ObjectUtils.convertTo(clazz, e);

            default:
                throw new AssertionError();
        }
    }

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

    void resetState();

    @Nullable
    Object objectWithInferredType(Object using, SerializationStrategy strategy, Class type);

    default UUID uuid() {
        return UUID.fromString(text());
    }
}
