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
import net.openhft.chronicle.bytes.PointerBytesStore;
import net.openhft.chronicle.bytes.ReadBytesMarshallable;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.*;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.*;

/**
 * Read in data after reading a field.
 */
public interface ValueIn {
    Consumer<ValueIn> DISCARD = v -> {
    };
    ThreadLocal<CharSequence> charSequenceThreadLocal = ThreadLocal.withInitial(StringBuilder::new);

    /*
     * Text / Strings.
     */
    default <T> WireIn text(T t, @NotNull BiConsumer<T, String> ts) {
        ts.accept(t, text());
        return wireIn();
    }

    default WireIn text(@NotNull StringBuilder sb) {
        if (textTo(sb) == null)
            sb.setLength(0);
        return wireIn();
    }

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
    WireIn bytes(@NotNull Bytes<?> toBytes);

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
        byte[] bytes = bytes();
        return bytes == null ? null : BytesStore.wrap(bytes);
    }

    default void byteBuffer(ByteBuffer bb) {
        bb.put(bytes());
    }

    @NotNull
    WireIn wireIn();

    /**
     * the length of the field as bytes including any encoding and header character
     */
    long readLength();

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

    boolean hasNext();

    boolean hasNextSequenceItem();

    @NotNull
    <T> WireIn uuid(@NotNull T t, @NotNull BiConsumer<T, UUID> tuuid);

    @NotNull
    <T> WireIn int64array(@Nullable LongArrayValues values, T t, @NotNull BiConsumer<T, LongArrayValues> setter);

    @NotNull
    WireIn int64(@NotNull LongValue value);

    default LongValue int64ForBinding(@Nullable LongValue value) {
        LongValue ret = wireIn().newLongReference();
        int64(ret);
        return ret;
    }

    @NotNull
    <T> WireIn int64(@Nullable LongValue value, T t, @NotNull BiConsumer<T, LongValue> setter);

    @NotNull
    <T> WireIn int32(@Nullable IntValue value, T t, @NotNull BiConsumer<T, IntValue> setter);

    @NotNull
    <T> WireIn sequence(@NotNull T t, @NotNull BiConsumer<T, ValueIn> tReader);

    default <T> Set<T> set(Class<T> t) {
        return collection(LinkedHashSet::new, t);
    }

    default <T> List<T> list(Class<T> t) {
        return collection(ArrayList::new, t);
    }

    default <T, C extends Collection<T>> C collection(Supplier<C> supplier, Class<T> t) {
        C list = supplier.get();
        sequence(list, (s, v) -> {
            while (v.hasNextSequenceItem())
                s.add(v.object(t));
        });
        return list;
    }

    default <O, T extends ReadMarshallable, C extends Set<T>> WireIn set(O o, Function<O, T> tSupplier) {
        return collection(o, tSupplier);
    }

    default <O, T extends ReadMarshallable, C extends List<T>> WireIn list(O o, Function<O, T> tSupplier) {
        return collection(o, tSupplier);
    }

    default <O, T extends ReadMarshallable, C extends Collection<T>> WireIn collection(O o, Function<O, T> tSupplier) {
        sequence(o, (o2, v) -> {
            while (v.hasNextSequenceItem()) {
                T t = tSupplier.apply(o2);
                v.marshallable(t);
            }
        });
        return wireIn();
    }

    default <V> Map<String, V> marshallableAsMap(Class<V> vClass) {
        return marshallableAsMap(vClass, LinkedHashMap<String, V>::new);
    }

    default <V> Map<String, V> marshallableAsMap(Class<V> vClass, Supplier<Map<String, V>> supplier) {
        Map<String, V> map = supplier.get();
        return marshallableAsMap(vClass, map);
    }

    default <V> Map<String, V> marshallableAsMap(Class<V> vClass, Map<String, V> map) {
        marshallable(m -> {
            StringBuilder sb = new StringBuilder();
            while (m.hasMore()) {
                m.readEventName(sb)
                        .object(vClass, map, (map2, v) -> map2.put(sb.toString(), v));
            }
        });
        return map;
    }

    <T> T applyToMarshallable(Function<WireIn, T> marshallableReader);

    @Nullable
    <T> T typedMarshallable() throws IORuntimeException;

    // todo peter to implement
    // <T> T typedMarshallable(Object context) throws IORuntimeException;

    @NotNull
    <T> ValueIn typePrefix(T t, @NotNull BiConsumer<T, CharSequence> ts);

    @NotNull
    <T> WireIn typeLiteralAsText(T t, @NotNull BiConsumer<T, CharSequence> classNameConsumer) throws IORuntimeException, BufferUnderflowException;

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

    @NotNull
    WireIn marshallable(@NotNull ReadMarshallable object) throws BufferUnderflowException, IORuntimeException;

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
     * @deprecated use marshallableAsMap
     */
    @Deprecated
    @Nullable
    <K, V> Map<K, V> map(@NotNull Class<K> kClazz,
                         @NotNull Class<V> vClass,
                         @NotNull Map<K, V> usingMap);

    boolean bool();

    byte int8();

    short int16();

    int uint16();

    int int32();

    long int64();

    double float64();

    float float32();

    <T> Class<T> typeLiteral() throws IORuntimeException, BufferUnderflowException;

    default Throwable throwable(boolean appendCurrentStack) {
        return WireInternal.throwable(this, appendCurrentStack);
    }

    default <E extends Enum<E>> E asEnum(Class<E> eClass) {
        StringBuilder sb = WireInternal.acquireStringBuilder();
        text(sb);
        return sb.length() == 0 ? null : WireInternal.internEnum(eClass, sb);
    }

    default <E extends Enum<E>> WireIn asEnum(Class<E> eClass, Consumer<E> eConsumer) {
        eConsumer.accept(asEnum(eClass));
        return wireIn();
    }

    default <E extends Enum<E>, T> WireIn asEnum(Class<E> eClass, T t, BiConsumer<T, E> teConsumer) {
        teConsumer.accept(t, asEnum(eClass));
        return wireIn();
    }

    @Nullable
    default <E> E object(@NotNull Class<E> clazz) {

        if (clazz == CharSequence.class)
            return object((E) charSequenceThreadLocal.get(), clazz);

        return object(null, clazz);
    }

    @Nullable
    default Object object() {
        return object(charSequenceThreadLocal.get(), Object.class);
    }

    @Nullable
    <E> E object(@Nullable E using, @NotNull Class<E> clazz);

    @Nullable
    <T, E> WireIn object(@NotNull Class<E> clazz, T t, BiConsumer<T, E> e);

    boolean isTyped();

    Class typePrefix();

    void resetState();
}
