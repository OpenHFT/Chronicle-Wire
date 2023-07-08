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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This functional interface defines methods for reading marshallable objects.
 * It can be used to parse a series of data from a source, where each unit of data is a marshallable object.
 * The size of the internal cache for interning strings can be configured via a system property "marshallableIn.intern.size".
 */
@FunctionalInterface
public interface MarshallableIn {
    int MARSHALLABLE_IN_INTERN_SIZE = Integer.getInteger("marshallableIn.intern.size", 128);

    /**
     * Read the next document from the data source.
     *
     * @return The document context which encapsulates the document read.
     */
    @NotNull
    DocumentContext readingDocument();

    /**
     * Reads the next document using the provided reader.
     *
     * @param reader The reader used to read the document.
     * @return {@code true} if a document was successfully read, {@code false} otherwise.
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
     * Reads the next set of bytes using the provided reader.
     *
     * @param reader The reader used to read the bytes.
     * @return {@code true} if bytes were successfully read, {@code false} otherwise.
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
     * Reads the next set of bytes into the provided Bytes object.
     *
     * @param using The Bytes object where the read bytes will be written.
     * @return {@code true} if bytes were successfully read, {@code false} otherwise.
     */
    @SuppressWarnings("rawtypes")
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
     * Reads the next message as a String.
     *
     * @return The message as a String or null if no message is present.
     */
    @Nullable
    default String readText() throws InvalidMarshallableException {
        try (@NotNull DocumentContext dc = readingDocument()) {
            if (!dc.isPresent()) {
                return null;
            }
            StringBuilder sb = WireInternal.acquireStringBuilder();
            dc.wire().getValueIn().text(sb);
            return sb.length() < MARSHALLABLE_IN_INTERN_SIZE
                    ? WireInternal.INTERNER.intern(sb)
                    : sb.toString();
        }
    }

    /**
     * Reads the next message into the provided StringBuilder.
     *
     * @param sb StringBuilder to copy the text into.
     * @return true if there was a message, or false if not.
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
     * Reads a Map<String, Object> from the content of the next message.
     *
     * @return The Map, or null if no message is waiting.
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
     * Reads messages from the tail of this queue as methods.
     *
     * @param objects The objects which implement the methods serialized to the file.
     * @return a reader which will read one Excerpt at a time
     */
    @NotNull
    default MethodReader methodReader(Object... objects) {
        return methodReaderBuilder().build(objects);
    }

    /**
     * Create a builder for a MethodReader.
     *
     * @return a new MethodReaderBuilder.
     */
    @NotNull
    default VanillaMethodReaderBuilder methodReaderBuilder() {
        return new VanillaMethodReaderBuilder(this);
    }
}
