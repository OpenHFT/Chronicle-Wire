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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.EOFException;
import java.io.ObjectInput;
import java.io.StreamCorruptedException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Defines the standard interface for reading sequentially from a Bytes stream.
 * <p>
 * Typical usage:
 * <pre>
 *   WireIn wire = new YamlWire(Bytes.from("key: value"));
 *   String v = wire.read("key").text();
 * </pre>
 * For a full example see the Chronicle Wire documentation.
 */
@DontChain
public interface WireIn extends WireCommon, MarshallableIn {

    /**
     * Consumes the remaining entries of the current document and returns them as a map.
     * Each entry is interpreted as a key followed by a value, typically from a YAML map or key/value event sequence.
     *
     * @param <K>     The type of keys in the map.
     * @param <V>     The type of values in the map.
     * @param kClass  The class type of the key.
     * @param vClass  The class type of the value.
     * @param map     The map to populate with read entries.
     * @return The populated map.
     * @throws InvalidMarshallableException If there's an error in the marshalling process.
     */
    @NotNull
    default <K, V> Map<K, V> readAllAsMap(Class<K> kClass, @NotNull Class<V> vClass, @NotNull Map<K, V> map) throws InvalidMarshallableException {
        while (isNotEmptyAfterPadding()) {
            long len = bytes().readRemaining();
            final K k = readEvent(kClass);
            @Nullable final V v = getValueIn()
                    .object(vClass);
            if (len == bytes().readRemaining())
                break;
            map.put(k, v);
        }
        return map;
    }

    /**
     * Copies all remaining readable bytes from this wire to another wire.
     * Only the unread portion of the current document is copied.
     *
     * @param wire The destination to copy the data to.
     * @throws InvalidMarshallableException If a marshalling error occurs.
     */
    void copyTo(@NotNull WireOut wire) throws InvalidMarshallableException;

    /**
     * Reads the next field name and returns a {@link ValueIn} for its value.
     * If the wire has no more fields the returned {@link ValueIn} will read a default value.
     */
    @NotNull
    ValueIn read();

    /**
     * Reads the field with the given key if present, skipping over fields that do not match.
     * Useful for binary formats where field order is not guaranteed.
     *
     * @param key The field name to search for.
     * @return The {@link ValueIn} for the matched field, or an empty value if not found.
     */
    @NotNull
    ValueIn read(@NotNull WireKey key);

    /**
     * Reads the field with the given name.
     *
     * @param fieldName The name of the field to locate.
     * @return The {@link ValueIn} for that field.
     */
    @NotNull
    default ValueIn read(String fieldName) {
        return read(() -> fieldName);
    }

    /**
     * Reads the identifier of the next event as a long.
     * Binary wires may encode method names as numbers, in which case this method returns that number,
     * otherwise {@link Long#MIN_VALUE} is returned.
     */
    long readEventNumber();

    /**
     * Reads an event or field name and always consumes a value.
     * Useful when reading RAW wires where the name may be omitted.
     *
     * @param name Holds the parsed name.
     * @return The {@link ValueIn} representing the associated value.
     * @throws IORuntimeException If the bytes cannot be parsed.
     */
    @NotNull
    default ValueIn readEventName(@NotNull StringBuilder name) {
        try {
            return read(name);
        } catch (Exception e) {
            String s;
            try {
                s = bytes().toDebugString(128);
            } catch (Throwable ex) {
                s = ex.toString();
            }
            throw new IORuntimeException("failed to parse bytes=" + s, e);
        }
    }

    /**
     * Populates {@code name} with the next field name and returns a {@link ValueIn} for its value.
     * If there is no field to read the returned {@link ValueIn} will be empty.
     *
     * @param name holder for the field name found on the wire
     * @return the {@link ValueIn} of that field
     */
    @NotNull
    ValueIn read(@NotNull StringBuilder name);

    /**
     * Reads the name of the next event and converts it to the requested type.
     * Commonly used when event names are enums.
     *
     * @param <K>           expected type of the event name
     * @param expectedClass class of the expected type
     * @return the converted name or {@code null} if absent
     * @throws InvalidMarshallableException if conversion fails
     */
    @Nullable <K> K readEvent(Class<K> expectedClass) throws InvalidMarshallableException;

    /**
     * Returns the {@link ValueIn} representing the value of the last field or event read.
     * Typically used after {@link #readEvent(Class)}.
     */
    @NotNull
    ValueIn getValueIn();

    /**
     * Provides a standard {@link ObjectInput} view of this wire for interoperability with
     * libraries expecting Java serialisation APIs.
     */
    ObjectInput objectInput();

    /**
     * Reads a comment and appends it to {@code sb}.
     * Useful for debugging and tooling.
     *
     * @param sb accumulator for the comment text
     * @return this wire for chaining
     */
    @NotNull
    WireIn readComment(@NotNull StringBuilder sb);

    /**
     * Clears the WireIn, effectively resetting its state.
     */
    @Override
    void clear();

    /**
     * Indicates whether the current document has more fields to read.
     * Padding bytes are consumed before the check is made.
     */
    default boolean hasMore() {
        return isNotEmptyAfterPadding();
    }

    /**
     * Consumes any trailing padding and checks whether unread data remains in the current document.
     */
    default boolean isNotEmptyAfterPadding() {
        consumePadding();
        return !isEmpty();
    }

    /**
     * Returns {@code true} if no more bytes are available to read.
     */
    default boolean isEmpty() {
        return bytes().isEmpty();
    }

    /**
     * Skips padding bytes so the next read starts at the given alignment.
     * Useful for binary structures that require fixed-size blocks.
     *
     * @param alignment the byte boundary to align to
     * @return this wire for chaining
     */
    @NotNull
    default WireIn readAlignTo(int alignment) {
        return this;
    }

    // TODO add a try-with-resource support for readDocument.

    /**
     * Reads a complete document and passes the metadata and data sections to the supplied consumers.
     * Both consumers may be {@code null} if the caller wishes to skip that portion.
     *
     * @param metaDataConsumer handler for metadata, may be {@code null}
     * @param dataConsumer     handler for data, may be {@code null}
     * @return {@code true} if a document was read
     * @throws InvalidMarshallableException if a marshalling error occurs
     */
    default boolean readDocument(@Nullable ReadMarshallable metaDataConsumer,
                                 @Nullable ReadMarshallable dataConsumer) throws InvalidMarshallableException {
        return WireInternal.readData(this, metaDataConsumer, dataConsumer);
    }

    /**
     * Reads a document starting at a known position.
     * Useful when random access to a wire store is required.
     *
     * @param position         absolute position of the document header
     * @param metaDataConsumer handler for metadata, may be {@code null}
     * @param dataConsumer     handler for data, may be {@code null}
     * @return {@code true} if a document was read
     * @throws InvalidMarshallableException if a marshalling error occurs
     */
    default boolean readDocument(long position,
                                 @Nullable ReadMarshallable metaDataConsumer,
                                 @Nullable ReadMarshallable dataConsumer) throws InvalidMarshallableException {
        return WireInternal.readData(position, this, metaDataConsumer, dataConsumer);
    }

    /**
     * Reads raw bytes into the provided {@code marshallable} without interpreting them.
     *
     * @param marshallable destination for the raw data
     * @throws InvalidMarshallableException if a marshalling error occurs
     */
    default void rawReadData(@NotNull ReadMarshallable marshallable) throws InvalidMarshallableException {
        WireInternal.rawReadData(this, marshallable);
    }

    /**
     * Equivalent to {@link WireIn#readDocument(ReadMarshallable, ReadMarshallable)} but without lambdas.
     * Returns a context representing the current document.
     */
    @Override
    @NotNull
    DocumentContext readingDocument();

    /**
     * Provides a {@link DocumentContext} for reading a document at {@code readLocation}.
     * This does not change the wire position.
     */
    DocumentContext readingDocument(long readLocation);

    /**
     * Consumes and discards padding bytes between the current read position and the next data item.
     */
    void consumePadding();

    /**
     * Registers a listener that receives comment text encountered while reading.
     *
     * @param commentListener consumer for comment strings
     */
    void commentListener(Consumer<CharSequence> commentListener);

    /**
     * Consumes a data header if one is available at the current position.
     * This is primarily used by queue and network layers to detect document boundaries.
     *
     * @return {@code true} if a data header was consumed
     * @throws EOFException if the end of wire marker is reached
     */
    default boolean readDataHeader() throws EOFException {
        return readDataHeader(false) == HeaderType.DATA;
    }

    /**
     * Reads a header from the wire and returns its {@link HeaderType}.
     * Setting {@code includeMetaData} to {@code true} allows metadata headers to be consumed.
     *
     * @param includeMetaData include metadata headers if {@code true}
     * @return the header type read, or {@link HeaderType#NONE}
     * @throws EOFException if an end-of-file marker is encountered
     */
    @NotNull
    HeaderType readDataHeader(boolean includeMetaData) throws EOFException;

    /**
     * Reads the length prefix at {@code position} and sets the read limit accordingly.
     * Used by {@link DocumentContext} implementations.
     *
     * @param position byte offset of the length field
     */
    void readAndSetLength(long position);

    /**
     * Reads the very first header in the stream, waiting up to the given timeout.
     *
     * @param timeout  how long to wait
     * @param timeUnit unit for the timeout
     * @throws TimeoutException         if the header does not arrive in time
     * @throws StreamCorruptedException if the stream is invalid
     */
    void readFirstHeader(long timeout, TimeUnit timeUnit) throws TimeoutException, StreamCorruptedException;

    /**
     * Reads the very first header in the stream without waiting.
     *
     * @throws StreamCorruptedException if the stream contains invalid data
     */
    void readFirstHeader() throws StreamCorruptedException;

    /**
     * Consumes a metadata header at the current position.
     */
    void readMetaDataHeader();

    /**
     * Returns a YAML formatted view of the bytes at the current position without consuming them.
     * Intended for debugging only.
     */
    String readingPeekYaml();

    /**
     * Marks the start of a nested event block.
     */
    default void startEvent() {
    }

    /**
     * Returns {@code true} when the current event block has ended.
     */
    default boolean isEndEvent() {
        return false;
    }

    /**
     * Marks the end of a nested event block.
     */
    default void endEvent() {
    }

    /**
     * Returns {@code true} if fields are expected in input order rather than random access.
     */
    default boolean hintReadInputOrder() {
        return false;
    }

    /**
     * Returns {@code true} if a metadata prefix is present at the current position.
     */
    default boolean hasMetaDataPrefix() {
        return false;
    }

    /**
     * Enumeration representing possible header types that can be read from a WireIn instance.
     */
    enum HeaderType {
        NONE,       // No header was found or read.
        DATA,       // Data header was found or read.
        META_DATA,  // Metadata header was found or read.
        EOF         // End-of-file marker was found or read.
    }
}
