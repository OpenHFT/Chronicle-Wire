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
 * Represents a data structure that supports both reading from and writing to marshallable
 * formats. Implementations of this interface can be converted to and from serialized forms,
 * making it suitable for storage, transmission, or other forms of data exchange.
 * <p>
 * This interface also provides a set of static utility methods to aid in the manipulation
 * and interpretation of marshallable data, allowing for data comparison, hashing,
 * and serialization to and from string and file representations.
 */
@DontChain
public interface Marshallable extends WriteMarshallable, ReadMarshallable, Resettable {

    /**
     * Compares a given {@code WriteMarshallable} object with another object for equality.
     *
     * @param $this The reference {@code WriteMarshallable} object.
     * @param o The object to compare with.
     * @return {@code true} if both objects are equal, {@code false} otherwise.
     */
    static boolean $equals(@NotNull WriteMarshallable $this, Object o) {
        return o instanceof WriteMarshallable &&
                ($this == o || Wires.isEquals($this, o));
    }

    /**
     * Generates a 32-bit hash code for a given {@code WriteMarshallable} object.
     *
     * @param $this The reference {@code WriteMarshallable} object.
     * @return The 32-bit hash code.
     */
    static int $hashCode(WriteMarshallable $this) {
        return HashWire.hash32($this);
    }

    /**
     * Converts a given {@code WriteMarshallable} object into its string representation.
     *
     * @param $this The reference {@code WriteMarshallable} object.
     * @return A string representation of the object.
     */
    static String $toString(WriteMarshallable $this) {
        return TEXT.asString($this);
    }

    /**
     * Converts a {@code CharSequence} into its corresponding marshallable representation.
     *
     * @param cs The character sequence to convert.
     * @return The corresponding marshallable object, or {@code null} if the conversion is not possible.
     */
    @Nullable
    static <T> T fromString(@NotNull CharSequence cs) throws InvalidMarshallableException {
        return TEXT.fromString(cs);
    }

    /**
     * Converts a {@code CharSequence} into its corresponding marshallable representation,
     * expecting a specific type as the result.
     *
     * @param tClass The expected class of the resulting object.
     * @param cs The character sequence to convert.
     * @return The corresponding marshallable object of type {@code T}, or {@code null} if the conversion is not possible.
     */
    @Nullable
    static <T> T fromString(@NotNull Class<T> tClass, @NotNull CharSequence cs) throws InvalidMarshallableException {
        return TEXT.fromString(tClass, cs);
    }

    /**
     * Reads a file, either from the current working directory or the classpath, and interprets its
     * content as a marshallable object. The method supports various file formats and relies on
     * the underlying marshallable parsing logic to interpret the content.
     *
     * @param filename The name or path of the file to read.
     * @return The marshallable object interpreted from the file content.
     */
    @NotNull
    static <T> T fromFile(String filename) throws IOException, InvalidMarshallableException {
        return TEXT.fromFile(filename);
    }

    /**
     * Converts the content from an {@code InputStream} into its corresponding marshallable representation.
     *
     * @param is The input stream containing the content to convert.
     * @return The corresponding marshallable object, or {@code null} if the conversion is not possible.
     */
    static <T> T fromString(@NotNull InputStream is) throws InvalidMarshallableException {
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return TEXT.fromString(s.hasNext() ? s.next() : "");
    }

    /**
     * Reads a file, either from the current working directory or the classpath, and interprets its
     * content as a marshallable object of a specific type.
     *
     * @param expectedType The expected type of the resulting object.
     * @param filename The name or path of the file to read.
     * @return The marshallable object interpreted from the file content.
     */
    @Nullable
    static <T> T fromFile(@NotNull Class<T> expectedType, String filename) throws IOException, InvalidMarshallableException {
        return TEXT.fromFile(expectedType, filename);
    }

    /**
     * Streams the content of a file as marshallable objects.
     *
     * @param filename The name or path of the file to read.
     * @return A stream of marshallable objects.
     */
    @NotNull
    static <T> Stream<T> streamFromFile(String filename) throws IOException {
        return TEXT.streamFromFile(filename);
    }

    /**
     * Streams the content of a file as marshallable objects of a specific type.
     *
     * @param expectedType The expected type of the resulting objects in the stream.
     * @param filename The name or path of the file to read.
     * @return A stream of marshallable objects of type {@code T}.
     */
    @Nullable
    static <T> Stream<T> streamFromFile(@NotNull Class<T> expectedType, String filename) throws IOException {
        return TEXT.streamFromFile(expectedType, filename);
    }

    /**
     * Retrieves the value of a specific field from the current marshallable object, expecting
     * a certain type for the field's value.
     *
     * @param name The name of the field.
     * @param tClass The expected class/type of the field's value.
     * @return The value of the specified field.
     */
    @Nullable
    default <T> T getField(String name, Class<T> tClass) throws NoSuchFieldException {
        return Wires.getField(this, name, tClass);
    }

    /**
     * Sets the value of a specific field in the current marshallable object.
     *
     * @param name The name of the field.
     * @param value The new value for the specified field.
     */
    default void setField(String name, Object value) throws NoSuchFieldException {
        Wires.setField(this, name, value);
    }

    /**
     * Retrieves the long value of a specific field from the current marshallable object.
     *
     * @param name The name of the field.
     * @return The long value of the specified field.
     */
    default long getLongField(String name) throws NoSuchFieldException {
        return Wires.getLongField(this, name);
    }

    /**
     * Sets the long value of a specific field in the current marshallable object.
     *
     * @param name The name of the field.
     * @param value The new long value for the specified field.
     */
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
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    default void readMarshallable(@NotNull WireIn wire) throws IORuntimeException, InvalidMarshallableException {
        // Obtain the WireMarshaller for the current class
        WireMarshaller wm = WIRE_MARSHALLER_CL.get(this.getClass());

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
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    default void writeMarshallable(@NotNull WireOut wire) throws InvalidMarshallableException {
        // Obtain the WireMarshaller for the current class
        WireMarshaller wm = WIRE_MARSHALLER_CL.get(this.getClass());

        // Delegate the writing process to the obtained WireMarshaller
        wm.writeMarshallable(this, wire);
    }

    /**
     * Creates a deep copy of the current marshallable object.
     *
     * @return The deep copy of the current object.
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
     * Merges the current marshallable object into a map, using a specified function to determine the key.
     *
     * @param map The map to merge into.
     * @param getKey The function to determine the key for the current object in the map.
     * @return The merged marshallable object in the map.
     */
    default <K, T extends Marshallable> T mergeToMap(@NotNull Map<K, T> map, @NotNull Function<T, K> getKey) {
        @NotNull @SuppressWarnings("unchecked")
        T t = (T) this;
        return map.merge(getKey.apply(t), t,
                Wires::copyTo);
    }

    /**
     * Retrieves the list of field information for the current class.
     *
     * @return A list of field information objects.
     */
    @NotNull
    default List<FieldInfo> $fieldInfos() {
        return Wires.fieldInfos(getClass());
    }

    /**
     * Retrieves a map of field information for the current class, with field names as keys.
     *
     * @return A map with field names as keys and field information objects as values.
     */
    default @NotNull Map<String, FieldInfo> $fieldInfoMap() {
        return Wires.fieldInfoMap(getClass());
    }

    /**
     * Returns the alias name for the current class, if available.
     *
     * @return The alias name for the current class or the canonical name if no alias exists.
     */
    default String className() {
        return ClassAliasPool.CLASS_ALIASES.nameFor(getClass());
    }

    /**
     * Resets the current marshallable object to its initial state.
     */
    default void reset() {
        Wires.reset(this);
    }
}
