/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
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

import net.openhft.chronicle.core.annotation.DontChain;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
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

import static net.openhft.chronicle.wire.WireMarshaller.WIRE_MARSHALLER_CL;
import static net.openhft.chronicle.wire.WireType.TEXT;

/**
 * The implementation of this interface is both readable and write-able as marshallable data.
 */
@DontChain
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
    static <T> T fromString(@NotNull CharSequence cs) throws InvalidMarshallableException {
        return TEXT.fromString(cs);
    }

    @Nullable
    static <T> T fromString(@NotNull Class<T> tClass, @NotNull CharSequence cs) throws InvalidMarshallableException {
        return TEXT.fromString(tClass, cs);
    }

    /**
     * Reads the file from the current working directory, or the class path.
     *
     * @param filename or path to read
     * @return the marshallable object
     */
    @NotNull
    static <T> T fromFile(String filename) throws IOException, InvalidMarshallableException {
        return TEXT.fromFile(filename);
    }

    static <T> T fromString(@NotNull InputStream is) throws  InvalidMarshallableException {
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
    static <T> T fromFile(@NotNull Class<T> expectedType, String filename) throws IOException, InvalidMarshallableException {
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

    @Nullable
    default <T> T getField(String name, Class<T> tClass) throws NoSuchFieldException {
        return Wires.getField(this, name, tClass);
    }

    default void setField(String name, Object value) throws NoSuchFieldException {
        Wires.setField(this, name, value);
    }

    default long getLongField(String name) throws NoSuchFieldException {
        return Wires.getLongField(this, name);
    }

    default void setLongField(String name, long value) throws NoSuchFieldException {
        Wires.setLongField(this, name, value);
    }

    /**
     * Reads the state of the Marshallable object from the given wire input. The method
     * obtains a WireMarshaller specific to the class of the current object and delegates
     * the reading process to that marshaller.
     * <p>
     * The default implementation will use a default value for each field not present
     *
     * @param wire The wire input source.
     * @throws IORuntimeException           If an IO error occurs during the read operation.
     * @throws InvalidMarshallableException If there's an error during marshalling.
     */
    @Override
    default void readMarshallable(@NotNull WireIn wire) throws IORuntimeException, InvalidMarshallableException {
        // Obtain the WireMarshaller for the current class
        WireMarshaller<Object> wm = WIRE_MARSHALLER_CL.get(this.getClass());

        // Delegate the reading process to the obtained WireMarshaller
        wm.readMarshallable(this, wire, true);
    }

    /**
     * Writes the state of the Marshallable object to the given wire output. The method
     * obtains a WireMarshaller specific to the class of the current object and delegates
     * the writing process to that marshaller.
     * <p>
     * The default implementation will write all values even if they are a default value. c.f. readMarshallable
     *
     * @param wire The wire output destination.
     * @throws InvalidMarshallableException If there's an error during marshalling.
     */
    @Override
    default void writeMarshallable(@NotNull WireOut wire) throws InvalidMarshallableException {
        // Obtain the WireMarshaller for the current class
        WireMarshaller<Object> wm = WIRE_MARSHALLER_CL.get(this.getClass());

        // Delegate the writing process to the obtained WireMarshaller
        wm.writeMarshallable(this, wire);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    default <T> T deepCopy() throws InvalidMarshallableException {
        return (T) Wires.deepCopy(this);
    }

    /**
     * Copy fields from this to dest by marshalling out and then in. Allows copying of fields by name
     * even if there is no type relationship between this and dest
     *
     * @param dest destination
     * @return t
     * @param <T> destination type
     */
    default <T extends Marshallable> T copyTo(@NotNull T dest) throws InvalidMarshallableException {
        return Wires.copyTo(this, dest);
    }

    default <K, T extends Marshallable> T mergeToMap(@NotNull Map<K, T> map, @NotNull Function<T, K> getKey) {
        @NotNull @SuppressWarnings("unchecked")
        T t = (T) this;
        return map.merge(getKey.apply(t), t,
                Wires::copyTo);
    }

    @NotNull
    default List<FieldInfo> $fieldInfos() {
        return Wires.fieldInfos(getClass());
    }

    default @NotNull Map<String, FieldInfo> $fieldInfoMap() {
        return Wires.fieldInfoMap(getClass());
    }

    default String className() {
        return ClassAliasPool.CLASS_ALIASES.nameFor(getClass());
    }

    default void reset() {
        Wires.reset(this);
    }
}
