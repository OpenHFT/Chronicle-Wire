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
import net.openhft.chronicle.core.io.Resettable;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Stream;

import static net.openhft.chronicle.wire.WireType.READ_ANY;
import static net.openhft.chronicle.wire.WireType.TEXT;

/**
 * The implementation of this interface is both readable and write-able as marshallable data.
 */
public interface Marshallable extends WriteMarshallable, ReadMarshallable, Resettable {
    static boolean $equals(@NotNull WriteMarshallable $this, Object o) {
        return o instanceof WriteMarshallable &&
                ($this == o || Wires.isEquals($this, o));
    }

    static int $hashCode(WriteMarshallable $this) {
        return HashWire.hash32($this);
    }

    static String $toString(WriteMarshallable $this) {
        return TEXT.asString($this);
    }

    @Nullable
    static <T> T fromString(@NotNull CharSequence cs) {
        return TEXT.fromString(cs);
    }

    @Nullable
    static <T> T fromString(@NotNull Class<T> tClass, @NotNull CharSequence cs) {
        return TEXT.fromString(tClass, cs);
    }

    /**
     * Reads the file from the current working directory, or the class path.
     *
     * @param filename or path to read
     * @return the marshallable object
     */
    @NotNull
    static <T> T fromFile(String filename) throws IOException {
        return TEXT.fromFile(filename);
    }

    static <T> T fromString(@NotNull InputStream is) {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return TEXT.fromString(s.hasNext() ? s.next() : "");
    }

    /**
     * Reads the file from the current working directory, or the class path.
     *
     * @param filename     or path to read
     * @param expectedType to deserialize as
     * @return the marshallable object
     */
    @Nullable
    static <T> T fromFile(@NotNull Class<T> expectedType, String filename) throws IOException {
        return TEXT.fromFile(expectedType, filename);
    }

    @NotNull
    static <T> Stream<T> streamFromFile(String filename) throws IOException {
        return TEXT.streamFromFile(filename);
    }

    @Nullable
    static <T> Stream<T> streamFromFile(@NotNull Class<T> expectedType, String filename) throws IOException {
        return TEXT.streamFromFile(expectedType, filename);
    }

    @Deprecated
    @NotNull
    static Map<String, Object> fromFileAsMap(String filename) throws IOException {
        return TEXT.fromFileAsMap(filename, Object.class);
    }

    @Deprecated
    @NotNull
    static <V> Map<String, V> fromFileAsMap(String filename, @NotNull Class<V> valueClass) throws IOException {
        return TEXT.fromFileAsMap(filename, valueClass);
    }

    @Deprecated
    @Nullable
    static Map<String, Object> fromHexString(@NotNull CharSequence cs) {
        return READ_ANY.fromHexString(cs);
    }

    @Nullable
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

    @NotNull
    default <T> T deepCopy() {
        return (T) Wires.deepCopy(this);
    }

    /* this method does the opposite of what the name suggests */
    @Deprecated
    @NotNull
    default <T extends Marshallable> T copyFrom(@NotNull T t) {
        return Wires.copyTo(this, t);
    }

    default <T extends Marshallable> T copyTo(@NotNull T t) {
        return Wires.copyTo(this, t);
    }

    default <K, T extends Marshallable> T mergeToMap(@NotNull Map<K, T> map, @NotNull Function<T, K> getKey) {
        @NotNull @SuppressWarnings("unchecked")
        T t = (T) this;
        return map.merge(getKey.apply(t), t,
                (p, c) -> p == null ? c.deepCopy() : p.copyFrom(c));
    }

    @NotNull
    default List<FieldInfo> $fieldInfos() {
        return Wires.fieldInfos(getClass());
    }

    default String getClassName() {
        return ClassAliasPool.CLASS_ALIASES.nameFor(getClass());
    }

    default void reset() {
        Wires.reset(this);
    }
}
