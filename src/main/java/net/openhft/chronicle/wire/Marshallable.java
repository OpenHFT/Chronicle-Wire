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
 * Primary interface for objects that can be written to and read from a {@link Wire}.
 * <p>
 * Implementations are typically simple data transfer objects or state holders that need to
 * be persisted or transmitted.  It combines the capabilities of
 * {@link WriteMarshallable} and {@link ReadMarshallable} and also extends
 * {@link net.openhft.chronicle.core.io.Resettable} so that instances can be recycled.
 * <p>
 * A number of convenience static methods are supplied for converting to and from textual
 * representations and for reflective style field access.
 */
@DontChain
public interface Marshallable extends WriteMarshallable, ReadMarshallable, Resettable {

    /**
     * Compare two {@link WriteMarshallable} instances for equality.
     * <p>
     * The comparison is performed on their serialized form via
     * {@link Wires#isEquals(Object, Object)} after verifying the other object is
     * also a {@code WriteMarshallable}.
     */
    static boolean $equals(@NotNull WriteMarshallable $this, Object o) {
        return o instanceof WriteMarshallable &&
                ($this == o || Wires.isEquals($this, o));
    }

    /**
     * Generate a 32‑bit hash code for the provided marshallable using
     * {@link HashWire#hash32(WriteMarshallable)}.
     */
    static int $hashCode(WriteMarshallable $this) {
        return HashWire.hash32($this);
    }

    /**
     * Convert the marshallable to a textual representation – typically YAML via
     * {@link WireType#TEXT}.
     */
    static String $toString(WriteMarshallable $this) {
        return TEXT.asString($this);
    }

    /**
     * Parse a textual representation (assumed {@link WireType#TEXT}) into a new
     * instance of the appropriate type.
     */
    @Nullable
    static <T> T fromString(@NotNull CharSequence cs) throws InvalidMarshallableException {
        return TEXT.fromString(cs);
    }

    /**
     * Parse a textual representation (assumed {@link WireType#TEXT}) into an
     * instance of {@code tClass}.
     */
    @Nullable
    static <T> T fromString(@NotNull Class<T> tClass, @NotNull CharSequence cs) throws InvalidMarshallableException {
        return TEXT.fromString(tClass, cs);
    }

    /**
     * Read a file (from the working directory or classpath) and deserialize its
     * textual content (typically YAML) into a marshallable instance.
     */
    @NotNull
    static <T> T fromFile(String filename) throws IOException, InvalidMarshallableException {
        return TEXT.fromFile(filename);
    }

    /**
     * Deserialize the entire contents of an {@link InputStream} using the default
     * textual format.
     */
    static <T> T fromString(@NotNull InputStream is) throws InvalidMarshallableException {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return TEXT.fromString(s.hasNext() ? s.next() : "");
    }

    /**
     * Read a file and deserialize it into an instance of {@code expectedType}
     * assuming a textual wire format.
     */
    @Nullable
    static <T> T fromFile(@NotNull Class<T> expectedType, String filename) throws IOException, InvalidMarshallableException {
        return TEXT.fromFile(expectedType, filename);
    }

    /**
     * Stream all documents contained in the given file, deserializing each using
     * the default textual format.
     */
    @NotNull
    static <T> Stream<T> streamFromFile(String filename) throws IOException {
        return TEXT.streamFromFile(filename);
    }

    /**
     * Stream all documents in the file as instances of {@code expectedType}.
     */
    @Nullable
    static <T> Stream<T> streamFromFile(@NotNull Class<T> expectedType, String filename) throws IOException {
        return TEXT.streamFromFile(expectedType, filename);
    }

    /**
     * Convenience method providing reflective-style access to a field value via
     * {@link Wires#getField(Object, String, Class)}.
     */
    @Nullable
    default <T> T getField(String name, Class<T> tClass) throws NoSuchFieldException {
        return Wires.getField(this, name, tClass);
    }

    /**
     * Set a field value reflectively using {@link Wires#setField(Object, String, Object)}.
     */
    default void setField(String name, Object value) throws NoSuchFieldException {
        Wires.setField(this, name, value);
    }

    /**
     * Shortcut to read a {@code long} field via {@link Wires#getLongField(Object, String)}.
     */
    default long getLongField(String name) throws NoSuchFieldException {
        return Wires.getLongField(this, name);
    }

    /**
     * Shortcut to write a {@code long} field via {@link Wires#setLongField(Object, String, long)}.
     */
    default void setLongField(String name, long value) throws NoSuchFieldException {
        Wires.setLongField(this, name, value);
    }

    /**
     * Read this object from {@code wire} using the
     * {@link WireMarshaller} associated with its class.  Subclasses rarely need
     * to override this unless they perform custom deserialization that cannot be
     * expressed via field marshalling.
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
     * Write this object to {@code wire} using the
     * {@link WireMarshaller} associated with its class.  Subclasses should only
     * override if they require bespoke serialization logic.
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
     * Create a deep copy by serialising this object to an in-memory binary wire
     * and deserialising it back into a new instance of the same class.  Enum
     * instances are returned as is.
     */
    @SuppressWarnings("unchecked")
    @NotNull
    default <T> T deepCopy() throws InvalidMarshallableException {
        return (T) Wires.deepCopy(this);
    }

    /**
     * Copy fields from this instance to {@code dest} by marshalling to a wire
     * and then reading into the destination.  Fields are matched by name so the
     * two objects do not need to share a type hierarchy.
     */
    default <T extends Marshallable> T copyTo(@NotNull T dest) throws InvalidMarshallableException {
        return Wires.copyTo(this, dest);
    }

    /**
     * Merge this object into {@code map}.  If an entry with the same key (as
     * provided by {@code getKey}) exists, its fields are updated using
     * {@link #copyTo(Marshallable)}; otherwise this instance is put into the map.
     */
    default <K, T extends Marshallable> T mergeToMap(@NotNull Map<K, T> map, @NotNull Function<T, K> getKey) {
        @NotNull @SuppressWarnings("unchecked")
        T t = (T) this;
        return map.merge(getKey.apply(t), t,
                Wires::copyTo);
    }

    /**
     * Return metadata describing the fields of this class as calculated by
     * {@link WireMarshaller}.  Useful for reflective operations.
     */
    @NotNull
    default List<FieldInfo> $fieldInfos() {
        return Wires.fieldInfos(getClass());
    }

    /**
     * As {@link #$fieldInfos()} but returned as a map keyed by field name.
     */
    default @NotNull Map<String, FieldInfo> $fieldInfoMap() {
        return Wires.fieldInfoMap(getClass());
    }

    /**
     * Returns the alias for this class as registered with
     * {@link net.openhft.chronicle.core.pool.ClassAliasPool#CLASS_ALIASES} or
     * falls back to the canonical class name.
     */
    default String className() {
        return ClassAliasPool.CLASS_ALIASES.nameFor(getClass());
    }

    /**
     * Reset this object to its default state via {@link Wires#reset(Object)}.
     */
    default void reset() {
        Wires.reset(this);
    }
}
