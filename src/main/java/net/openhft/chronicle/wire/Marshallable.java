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
 * Primary interface for objects that can be written to and read from a wire
 * format.  It combines the contracts of {@link WriteMarshallable} and
 * {@link ReadMarshallable} and also extends {@link Resettable} so that
 * instances may be reused.  Implementations are typically data transfer
 * objects or stateful components that require wire persistence or
 * transmission.  Utility methods are provided for common operations such as
 * equality checks and converting to or from a textual form.
 */
@DontChain
public interface Marshallable extends WriteMarshallable, ReadMarshallable, Resettable {

    /**
     * Compares two {@link WriteMarshallable} instances based on their
     * serialised form using {@link Wires#isEquals(Object, Object)}.  The
     * comparison first checks whether {@code o} is also a {@code
     * WriteMarshallable}.
     */
    static boolean $equals(@NotNull WriteMarshallable $this, Object o) {
        return o instanceof WriteMarshallable &&
                ($this == o || Wires.isEquals($this, o));
    }

    /**
     * Generates a 32-bit hash for the given {@link WriteMarshallable}, usually
     * derived from its serialised form via {@link HashWire#hash32(WriteMarshallable)}.
     */
    static int $hashCode(WriteMarshallable $this) {
        return HashWire.hash32($this);
    }

    /**
     * Serialises {@code $this} to a textual wire format (typically YAML) and
     * returns the result as a {@link String}.
     */
    static String $toString(WriteMarshallable $this) {
        return TEXT.asString($this);
    }

    /**
     * Deserialises the text in {@code cs} using the default textual
     * {@link WireType} (typically YAML).
     */
    @Nullable
    static <T> T fromString(@NotNull CharSequence cs) throws InvalidMarshallableException {
        return TEXT.fromString(cs);
    }

    /**
     * As {@link #fromString(CharSequence)} but returns an instance of
     * {@code tClass}.
     */
    @Nullable
    static <T> T fromString(@NotNull Class<T> tClass, @NotNull CharSequence cs) throws InvalidMarshallableException {
        return TEXT.fromString(tClass, cs);
    }

    /**
     * Deserialises {@code filename} from the working directory or classpath
     * using the default text format.
     */
    @NotNull
    static <T> T fromFile(String filename) throws IOException, InvalidMarshallableException {
        return TEXT.fromFile(filename);
    }

    /**
     * Reads the entire {@link InputStream} and deserialises it using the
     * default text format.
     */
    static <T> T fromString(@NotNull InputStream is) throws InvalidMarshallableException {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return TEXT.fromString(s.hasNext() ? s.next() : "");
    }

    /**
     * As {@link #fromFile(String)} but the result is typed as
     * {@code expectedType}.
     */
    @Nullable
    static <T> T fromFile(@NotNull Class<T> expectedType, String filename) throws IOException, InvalidMarshallableException {
        return TEXT.fromFile(expectedType, filename);
    }

    /**
     * Streams multiple documents from {@code filename} using the default
     * text format.
     */
    @NotNull
    static <T> Stream<T> streamFromFile(String filename) throws IOException {
        return TEXT.streamFromFile(filename);
    }

    /**
     * Variant of {@link #streamFromFile(String)} returning objects cast to
     * {@code expectedType}.
     */
    @NotNull
    static <T> Stream<T> streamFromFile(@NotNull Class<T> expectedType, String filename) throws IOException {
        return TEXT.streamFromFile(expectedType, filename);
    }

    /**
     * Convenience method providing reflective-style access to a field.
     */
    @Nullable
    default <T> T getField(String name, Class<T> tClass) throws NoSuchFieldException {
        return Wires.getField(this, name, tClass);
    }

    /**
     * Convenience setter mirroring {@link #getField(String, Class)}.
     */
    default void setField(String name, Object value) throws NoSuchFieldException {
        Wires.setField(this, name, value);
    }

    /**
     * Shorthand for {@link #getField(String, Class)} with {@code long}.
     */
    default long getLongField(String name) throws NoSuchFieldException {
        return Wires.getLongField(this, name);
    }

    /**
     * Shorthand for {@link #setField(String, Object)} with {@code long}.
     */
    default void setLongField(String name, long value) throws NoSuchFieldException {
        Wires.setLongField(this, name, value);
    }

    /**
     * Populates this instance from {@code wire}.  The default implementation
     * delegates to the {@link WireMarshaller} for the concrete class and rarely
     * needs to be overridden.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    default void readMarshallable(@NotNull WireIn wire) throws IORuntimeException, InvalidMarshallableException {
        // Obtain the WireMarshaller for the current class
        WireMarshaller wm = WIRE_MARSHALLER_CL.get(this.getClass());

        // Delegate the reading process to the obtained WireMarshaller
        wm.readMarshallable(this, wire, true);
    }

    /**
     * Serialises this instance to {@code wire}.  Like
     * {@link #readMarshallable(WireIn)} it uses the generated
     * {@link WireMarshaller} and typically does not need overriding.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    default void writeMarshallable(@NotNull WireOut wire) throws InvalidMarshallableException {
        // Obtain the WireMarshaller for the current class
        WireMarshaller wm = WIRE_MARSHALLER_CL.get(this.getClass());

        // Delegate the writing process to the obtained WireMarshaller
        wm.writeMarshallable(this, wire);
    }

    /**
     * Creates a deep copy by serialising this instance to an in-memory binary
     * wire and then reading it back.  Enum values are returned as-is.
     */
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

    /**
     * Merge this instance into {@code map}.  If an entry with the same key
     * already exists its fields are updated via {@link #copyTo(Marshallable)};
     * otherwise this instance is added.
     */
    default <K, T extends Marshallable> T mergeToMap(@NotNull Map<K, T> map, @NotNull Function<T, K> getKey) {
        @NotNull @SuppressWarnings("unchecked")
        T t = (T) this;
        return map.merge(getKey.apply(t), t,
                Wires::copyTo);
    }

    /**
     * Returns metadata describing the declared fields of this class.
     */
    @NotNull
    default List<FieldInfo> $fieldInfos() {
        return Wires.fieldInfos(getClass());
    }

    /**
     * Map variant of {@link #$fieldInfos()} indexed by field name.
     */
    default @NotNull Map<String, FieldInfo> $fieldInfoMap() {
        return Wires.fieldInfoMap(getClass());
    }

    /**
     * Returns the name used for this class in
     * {@link ClassAliasPool#CLASS_ALIASES} or the canonical name if no alias is
     * registered.
     */
    default String className() {
        return ClassAliasPool.CLASS_ALIASES.nameFor(getClass());
    }

    /**
     * Reset fields to their default values via {@link Wires#reset(Object)}.
     */
    default void reset() {
        Wires.reset(this);
    }
}
