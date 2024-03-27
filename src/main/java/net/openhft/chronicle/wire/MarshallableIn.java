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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.ReadBytesMarshallable;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.scoped.ScopedResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This interface provides methods for reading marshallable objects. It defines the contract for any
 * entity that is capable of reading marshalled data. The interface incorporates default methods for
 * various reading operations to offer flexibility and extensibility to implementors.
 * <p>
 * Note: This interface assumes that you are familiar with the concept of marshallable objects,
 * which are objects that can be marshalled (converted to byte streams) and unmarshalled
 * (converted back to objects).
 */
@FunctionalInterface
public interface MarshallableIn {

    // Size configuration for internal use
    int MARSHALLABLE_IN_INTERN_SIZE = Integer.getInteger("marshallableIn.intern.size", 128);

    /**
     * Provides a {@code DocumentContext} that can be used to read data from a source. The specific
     * source and the means of reading are determined by the concrete implementation.
     *
     * @return A {@code DocumentContext} appropriate for reading from the underlying source.
     */
    @NotNull
    DocumentContext readingDocument();

    /**
     * Reads a document using the provided {@code ReadMarshallable} instance. This method
     * simplifies the reading operation by handling the lifecycle of the {@code DocumentContext}.
     *
     * @param reader The mechanism to use for reading the document.
     * @return {@code true} if the reading operation was successful, otherwise {@code false}.
     */
    default boolean readDocument(@NotNull ReadMarshallable reader) throws InvalidMarshallableException {
        try (@NotNull DocumentContext dc = readingDocument()) {
            if (!dc.isPresent())
                return false;
            reader.readMarshallable(dc.wire());
        }
        return true;
    }

    /**
     * Reads bytes using the provided {@code ReadBytesMarshallable} instance. This method handles
     * the extraction of bytes from the source and offers them to the reader for processing.
     *
     * @param reader The mechanism to use for reading the bytes.
     * @return {@code true} if the reading operation was successful, otherwise {@code false}.
     */
    default boolean readBytes(@NotNull ReadBytesMarshallable reader) throws InvalidMarshallableException {
        try (@NotNull DocumentContext dc = readingDocument()) {
            if (!dc.isPresent())
                return false;
            reader.readMarshallable(dc.wire().bytes());
        }
        return true;
    }

    /**
     * Reads bytes from the source into the provided {@code Bytes} object. This method allows for
     * direct population of a {@code Bytes} instance without additional translation or processing.
     *
     * @param using The {@code Bytes} object to populate with read data.
     * @return {@code true} if the reading operation was successful, otherwise {@code false}.
     */
    default boolean readBytes(@NotNull Bytes<?> using) throws InvalidMarshallableException {
        try (@NotNull DocumentContext dc = readingDocument()) {
            if (!dc.isPresent())
                return false;
            Bytes<?> bytes = dc.wire().bytes();
            long len = Math.min(using.writeRemaining(), bytes.readRemaining());
            using.write(bytes, bytes.readPosition(), len);
            bytes.readSkip(len);
        }
        return true;
    }

    /**
     * Retrieves the next available message as a {@code String} from the underlying source. This
     * method aims to simplify string-based message reading and provides the capability to
     * handle large messages efficiently using string interning.
     *
     * @return The message as a {@code String} if available, otherwise {@code null}.
     */
    @Nullable
    default String readText() throws InvalidMarshallableException {
        try (@NotNull DocumentContext dc = readingDocument()) {
            if (!dc.isPresent()) {
                return null;
            }
            try (ScopedResource<StringBuilder> stlSb = Wires.acquireStringBuilderScoped()) {
                StringBuilder sb = stlSb.get();
                dc.wire().getValueIn().text(sb);
                return sb.length() < MARSHALLABLE_IN_INTERN_SIZE
                        ? WireInternal.INTERNER.intern(sb)
                        : sb.toString();
            }
        }
    }

    /**
     * Reads the next available message and populates the provided {@code StringBuilder} with
     * its contents. This method offers flexibility for clients that wish to control the
     * string allocation and reuse the same {@code StringBuilder} for multiple reads.
     *
     * @param sb The {@code StringBuilder} instance to populate with the message content.
     * @return {@code true} if a message was read and the {@code StringBuilder} was populated,
     *         otherwise {@code false}.
     */
    default boolean readText(@NotNull StringBuilder sb) throws InvalidMarshallableException {
        try (@NotNull DocumentContext dc = readingDocument()) {
            if (!dc.isPresent()) {
                sb.setLength(0);
                return false;
            }
            dc.wire().getValueIn().text(sb);
        }
        return true;
    }

    /**
     * Retrieves the next available message and interprets it as a {@code Map} with key-value pairs.
     * The method is designed to be generic, allowing clients to specify the expected key and value types
     * at the call site. If no message is available or the content does not represent a map, appropriate
     * fallbacks like {@code null} or an empty map are returned.
     *
     * @param <K> The expected type of the keys in the map.
     * @param <V> The expected type of the values in the map.
     * @return A {@code Map} constructed from the message, {@code null} if no message is available, or
     *         an empty map if the content does not represent a map.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    default <K, V> Map<K, V> readMap() throws InvalidMarshallableException {
        try (@NotNull DocumentContext dc = readingDocument()) {
            if (!dc.isPresent()) {
                return null;
            }
            final Wire wire = dc.wire();
            if (!wire.hasMore())
                return Collections.emptyMap();
            @NotNull Map<K, V> ret = new LinkedHashMap<>();
            while (wire.hasMore()) {
                @NotNull K key = (K) wire.readEvent(Object.class);
                @Nullable V value = (V) wire.getValueIn().object();
                ret.put(key, value);
            }
            return ret;
        }
    }

    /**
     * Constructs a {@code MethodReader} that can interpret and invoke methods based on the messages
     * present in this reader. The constructed reader will interpret each message as a method call and
     * will use the provided objects to invoke the corresponding methods.
     *
     * @param objects Objects that implement the methods serialized to the file. These objects will be
     *                used as the targets for method invocation based on the interpreted messages.
     * @return A {@code MethodReader} set up to read and interpret method calls from this reader's messages.
     */
    @NotNull
    default MethodReader methodReader(Object... objects) {
        return methodReaderBuilder().build(objects);
    }

    /**
     * Provides a builder instance that can be used to construct and configure a {@code MethodReader}
     * suitable for this reader's content. The builder offers flexibility in configuring the
     * {@code MethodReader} to suit specific requirements.
     *
     * @return A {@code VanillaMethodReaderBuilder} for creating and customizing a {@code MethodReader}.
     */
    @NotNull
    default VanillaMethodReaderBuilder methodReaderBuilder() {
        return new VanillaMethodReaderBuilder(this);
    }
}
