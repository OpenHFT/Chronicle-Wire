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
 * Abstraction for any source from which documents or messages can be read. Typical
 * implementations include queue tailers or network sockets. Default methods are provided to
 * make consumption convenient across different sources.
 */
@FunctionalInterface
public interface MarshallableIn {

    /**
     * Maximum length of a string that will be interned when read via
     * {@link #readText()}. Strings longer than this are returned without
     * interning. Controlled by system property {@code marshallableIn.intern.size}.
     */
    int MARSHALLABLE_IN_INTERN_SIZE = Integer.getInteger("marshallableIn.intern.size", 128);

    /**
     * Opens a {@link DocumentContext} for reading the next document or message from this input
     * source. Always use the returned context within a try-with-resources block to ensure that it
     * is closed.
     *
     * @return the context representing the next document
     */
    @NotNull
    DocumentContext readingDocument();

    /**
     * Convenience wrapper that opens a {@link DocumentContext}, delegates to the supplied reader to
     * consume its contents and closes the context afterwards.
     *
     * @param reader strategy used to read the document
     * @return {@code false} if no document was present
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
     * Opens a {@link DocumentContext} and allows the supplied reader to read the raw bytes of the
     * document. The context is closed once the reader has consumed the bytes.
     *
     * @param reader consumer of the document bytes
     * @return {@code false} if no document was available
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
     * Reads the raw bytes of the next document into the supplied {@code Bytes} instance.
     *
     * @param using destination for the bytes
     * @return {@code false} if no document was available
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
     * Reads the next document as a {@code String}. Strings shorter than
     * {@link #MARSHALLABLE_IN_INTERN_SIZE} may be interned for reuse.
     *
     * @return the text or {@code null} if no document was present
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
     * Reads the next document into the supplied {@code StringBuilder}.
     *
     * @param sb builder to populate
     * @return {@code false} if no document was present
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
     * Reads the next document as a map structure.
     *
     * @param <K> expected key type
     * @param <V> expected value type
     * @return {@code null} if no document was present, otherwise a possibly empty map
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
     * Creates a {@link MethodReader} that deserialises method calls from this input and dispatches
     * them to the supplied {@code objects}.
     *
     * @param objects targets for the method calls
     * @return a configured {@code MethodReader}
     */
    @NotNull
    default MethodReader methodReader(Object... objects) {
        return methodReaderBuilder().build(objects);
    }

    /**
     * Returns a {@link VanillaMethodReaderBuilder} for configuring a
     * {@link MethodReader} to consume this input.
     */
    @NotNull
    default VanillaMethodReaderBuilder methodReaderBuilder() {
        return new VanillaMethodReaderBuilder(this);
    }
}
