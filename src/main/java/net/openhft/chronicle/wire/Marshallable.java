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

import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static net.openhft.chronicle.wire.WireType.READ_ANY;
import static net.openhft.chronicle.wire.WireType.TEXT;

/**
 * The implementation of this interface is both readable and write-able as marshallable data.
 */
public interface Marshallable extends WriteMarshallable, ReadMarshallable {
    static boolean $equals(WriteMarshallable $this, Object o) {
        return o instanceof WriteMarshallable &&
                ($this == o || Wires.isEquals($this, o));
    }

    static int $hashCode(WriteMarshallable $this) {
        return HashWire.hash32($this);
    }

    static String $toString(WriteMarshallable $this) {
        return TEXT.asString($this);
    }

    static <T> T fromString(CharSequence cs) {
        return TEXT.fromString(cs);
    }

    static <T> T fromFile(String filename) throws IOException {
        return TEXT.fromFile(filename);
    }

    static <T> T fromFile(Class<T> expectedType, String filename) throws IOException {
        return TEXT.fromFile(expectedType, filename);
    }

    static Map<String, Object> fromFileAsMap(String filename) throws IOException {
        return TEXT.fromFileAsMap(filename, Object.class);
    }

    static <V> Map<String, V> fromFileAsMap(String filename, Class<V> valueClass) throws IOException {
        return TEXT.fromFileAsMap(filename, valueClass);
    }

    static Map<String, Object> fromHexString(CharSequence cs) {
        return READ_ANY.fromHexString(cs);
    }

    default <T> T getField(String name, Class<T> tClass) throws NoSuchFieldException {
        return Wires.getField(this, name, tClass);
    }

    default void setField(String name, Object value) throws NoSuchFieldException {
        Wires.setField(this, name, value);
    }

    @Override
    default void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        Wires.readMarshallable(this, wire, true);
    }

    @Override
    default void writeMarshallable(@NotNull WireOut wire) {
        Wires.writeMarshallable(this, wire);
    }

    default <T> T deepCopy() {
        return (T) Wires.deepCopy(this);
    }

    default <T extends Marshallable> T copyFrom(T t) {
        return Wires.copyTo(this, t);
    }

    default <K, T extends Marshallable> T mergeToMap(Map<K, T> map, Function<T, K> getKey) {
        @SuppressWarnings("unchecked")
        T t = (T) this;
        return map.merge(getKey.apply(t), t,
                (p, c) -> p == null ? c.deepCopy() : p.copyFrom(c));
    }

    default List<FieldInfo> $fieldInfos() {
        return Wires.fieldInfos(getClass());
    }

  /*  static <T extends Marshallable & KeyedMarshallable> T deepCopy(T t) {
        return (T) Wires.deepCopy(t);
    }*/
}
