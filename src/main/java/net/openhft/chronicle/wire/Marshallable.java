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
 * A cornerstone interface for objects that need to be serialised to and
 * deserialised from a wire format.  It combines the ability to {@link
 * WriteMarshallable write} and {@link ReadMarshallable read} and also extends
 * {@link Resettable} so that implementations may be reused.  Typical
 * implementations are data transfer objects or stateful components that must be
 * persisted or transmitted.  A set of utility methods is provided for common
 * operations such as comparison, hashing and conversion to and from textual
 * forms.
 */
@DontChain
public interface Marshallable extends WriteMarshallable, ReadMarshallable, Resettable {

    /**
     * Compares two {@link WriteMarshallable} instances.  The comparison first
     * checks that {@code o} is a {@link WriteMarshallable} and then delegates to
     * {@link Wires#isEquals(Object, Object)} to compare their serialised forms.
     */
    static boolean $equals(@NotNull WriteMarshallable $this, Object o) {
        return o instanceof WriteMarshallable &&
                ($this == o || Wires.isEquals($this, o));
    }

    /**
     * Generates a 32-bit hash of the supplied {@link WriteMarshallable}.  The
     * value is derived from the serialised form via
     * {@link HashWire#hash32(WriteMarshallable)}.
     */
    static int $hashCode(WriteMarshallable $this) {
        return HashWire.hash32($this);
    }

    /**
     * Serialises the supplied {@link WriteMarshallable} to a textual wire, for
     * example YAML via {@link WireType#TEXT}, and returns the resulting string.
     */
    static String $toString(WriteMarshallable $this) {
        return TEXT.asString($this);
    }

    /**
     * Parses a textual representation, usually YAML via {@link WireType#TEXT},
     * and returns the resulting object.
     */
    @Nullable
    static <T> T fromString(@NotNull CharSequence cs) throws InvalidMarshallableException {
        return TEXT.fromString(cs);
    }

    /**
     * Parses a textual representation into an instance of the supplied class,
     * assuming the text is in the default format ({@link WireType#TEXT}).
     */
    @Nullable
    static <T> T fromString(@NotNull Class<T> tClass, @NotNull CharSequence cs) throws InvalidMarshallableException {
        return TEXT.fromString(tClass, cs);
    }

    /**
     * Loads a file, usually encoded in the default textual format, and parses it
     * into a marshallable object.
     */
    @NotNull
    static <T> T fromFile(String filename) throws IOException, InvalidMarshallableException {
        return TEXT.fromFile(filename);
    }

    /**
     * Reads the entire {@link InputStream} and deserialises its textual content
     * using the default format.
     */
    static <T> T fromString(@NotNull InputStream is) throws InvalidMarshallableException {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return TEXT.fromString(s.hasNext() ? s.next() : "");
    }

    /**
     * Loads a file and converts it to the specified type using the default
     * textual wire format.
     */
    @Nullable
    static <T> T fromFile(@NotNull Class<T> expectedType, String filename) throws IOException, InvalidMarshallableException {
        return TEXT.fromFile(expectedType, filename);
    }

    /**
     * Returns a stream of objects read from the supplied file.  The file may
     * contain multiple documents separated according to the textual wire
     * format.
     */
    @NotNull
    static <T> Stream<T> streamFromFile(String filename) throws IOException {
        return TEXT.streamFromFile(filename);
    }

    /**
     * Returns a stream of objects of the given type read from the supplied file.
     * Multiple documents may be delimited according to the default text format.
     */
    @NotNull
    static <T> Stream<T> streamFromFile(@NotNull Class<T> expectedType, String filename) throws IOException {
        return TEXT.streamFromFile(expectedType, filename);
    }

    /**
     * Convenience method providing reflective-like access to a field's value via
     * {@link Wires}.  Useful for dynamic interaction but slower than generated
     * code.
     */
    @Nullable
    default <T> T getField(String name, Class<T> tClass) throws NoSuchFieldException {
        return Wires.getField(this, name, tClass);
    }

    /**
     * Sets a field using the reflective utilities in {@link Wires}.
     */
    default void setField(String name, Object value) throws NoSuchFieldException {
        Wires.setField(this, name, value);
    }

    /**
     * Convenience method to read a {@code long} field via {@link Wires}.
     */
    default long getLongField(String name) throws NoSuchFieldException {
        return Wires.getLongField(this, name);
    }

    /**
     * Convenience method to write a {@code long} field via {@link Wires}.
     */
    default void setLongField(String name, long value) throws NoSuchFieldException {
        Wires.setLongField(this, name, value);
    }

    /**
     * Deserialises this object from the supplied {@link WireIn}.  The default
     * implementation obtains the {@link WireMarshaller} for the class and uses
     * it to populate the fields.  Only override this when a custom wire format
     * is required.
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
     * Serialises this object to the supplied {@link WireOut}.  The default
     * implementation delegates to the {@link WireMarshaller} for the class and
     * writes every field.  Override only for non-standard behaviour.
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
     * Creates a deep copy of this object by serialising it to an in-memory
     * binary wire and then reading back a new instance.  Enum classes are
     * returned as-is.
     */
    @SuppressWarnings("unchecked")
    @NotNull
    default <T> T deepCopy() throws InvalidMarshallableException {
        return (T) Wires.deepCopy(this);
    }

    /**
     * Copies fields from this object to {@code dest} by serialising this
     * instance and reading the data into the destination.  Fields are matched by
     * name so the classes need not be related.
     */
    default <T extends Marshallable> T copyTo(@NotNull T dest) throws InvalidMarshallableException {
        return Wires.copyTo(this, dest);
    }

    /**
     * Merges this object into {@code map}.  If an entry with the same key exists
     * the existing value is updated via {@link #copyTo(Marshallable)}, otherwise
     * this instance is added.
     */
    default <K, T extends Marshallable> T mergeToMap(@NotNull Map<K, T> map, @NotNull Function<T, K> getKey) {
        @NotNull @SuppressWarnings("unchecked")
        T t = (T) this;
        return map.merge(getKey.apply(t), t,
                Wires::copyTo);
    }

    /**
     * Returns metadata describing the fields of this class as determined by
     * {@link WireMarshaller}.
     */
    @NotNull
    default List<FieldInfo> $fieldInfos() {
        return Wires.fieldInfos(getClass());
    }

    /**
     * Returns field metadata in a map keyed by field name.
     */
    default @NotNull Map<String, FieldInfo> $fieldInfoMap() {
        return Wires.fieldInfoMap(getClass());
    }

    /**
     * Returns the alias for this class as registered with
     * {@link ClassAliasPool#CLASS_ALIASES}, or the canonical name if no alias is
     * present.
     */
    default String className() {
        return ClassAliasPool.CLASS_ALIASES.nameFor(getClass());
    }

    /**
     * Resets this object to its default state as if newly constructed.  The
     * default implementation delegates to {@link Wires#reset(Object)}.
     */
    default void reset() {
        Wires.reset(this);
    }
}
