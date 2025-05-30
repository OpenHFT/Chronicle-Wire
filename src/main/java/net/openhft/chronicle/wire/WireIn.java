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
 * Defines the standard interface for reading sequentially from a {@link
 * net.openhft.chronicle.bytes.Bytes} stream.  Concrete implementations know how
 * to parse a specific wire format (text, binary, field-less binary, etc.) but
 * present a consistent API to clients.  Typical usage is to obtain a
 * {@link DocumentContext} via {@link #readingDocument()} and read fields via the
 * {@code read()} or {@code readEventName()} methods.
 */
@DontChain
public interface WireIn extends WireCommon, MarshallableIn {

    /**
     * Convenience method to consume the remainder of the current document as a
     * map.  Each key/value pair is read in sequence until no more data is
     * available.
     *
     * @param <K>    key type
     * @param <V>    value type
     * @param kClass class of the key
     * @param vClass class of the value
     * @param map    destination map to populate
     * @return the populated {@code map}
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
     * Writes the remaining readable data from this wire into another
     * {@link WireOut}.  This is a utility method used when reserializing a
     * document or forwarding it to another destination.
     */
    void copyTo(@NotNull WireOut wire) throws InvalidMarshallableException;

    /**
     * Begins reading the next field or sequence element.  For text formats this
     * also consumes the field name if present.  Binary formats may simply return
     * the next value.
     *
     * @return {@link ValueIn} used to read the value
     */
    @NotNull
    ValueIn read();

    /**
     * Reads the field identified by {@code key}.  Fields that do not match may
     * be skipped internally until the requested key is found.
     */
    @NotNull
    ValueIn read(@NotNull WireKey key);

    /**
     * Convenience overload of {@link #read(WireKey)} using a simple field name.
     */
    @NotNull
    default ValueIn read(String fieldName) {
        return read(() -> fieldName);
    }

    /**
     * Reads a numeric identifier for the next event.  Binary wires sometimes
     * encode events as integers rather than textual names.  If no such identifier
     * is present {@code Long.MIN_VALUE} is returned.
     */
    long readEventNumber();

    /**
     * Reads the event or field name into {@code name} and returns a
     * {@link ValueIn} for its value.  This method always consumes a value even if
     * the underlying format does not explicitly encode the name (e.g. RAW).
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
     * Reads a specific field based on the provided field name.
     * If the field is not present, it returns an empty string.
     *
     * @param name The name of the field to read.
     * @return The value of the specified field, encapsulated in a {@link ValueIn} instance.
     */
    @NotNull
    ValueIn read(@NotNull StringBuilder name);

    /**
     * Reads a field which may contain an object of any specified type.
     *
     * @param <K>           The type of object expected.
     * @param expectedClass The class type hint of the expected object. If no hint is available, use Object.class.
     * @return An instance of the specified expectedClass.
     * @throws InvalidMarshallableException If there's an error in the marshalling process.
     */
    @Nullable <K> K readEvent(Class<K> expectedClass) throws InvalidMarshallableException;

    /**
     * Obtains the value associated with a field or event for more advanced use cases.
     * Typically used after a call to {@link #readEvent(Class)}.
     *
     * @return The value of the field or event, encapsulated in a {@link ValueIn} instance.
     */
    @NotNull
    ValueIn getValueIn();

    /**
     * Returns the ObjectInput associated with this WireIn for serialization operations.
     *
     * @return the ObjectInput instance.
     */
    ObjectInput objectInput();

    /**
     * Reads a comment from the Wire data and appends it to the provided StringBuilder.
     *
     * @param sb StringBuilder to which the comment will be appended.
     * @return the WireIn instance for method chaining.
     */
    @NotNull
    WireIn readComment(@NotNull StringBuilder sb);

    /**
     * Clears the WireIn, effectively resetting its state.
     */
    @Override
    void clear();

    /**
     * Indicates whether more fields/events remain in the current document.  Any
     * trailing padding bytes are consumed before the check is made.
     */
    default boolean hasMore() {
        return isNotEmptyAfterPadding();
    }

    /**
     * Helper used by {@link #hasMore()} which consumes any alignment padding and
     * then checks if unread bytes remain in this document.
     */
    default boolean isNotEmptyAfterPadding() {
        consumePadding();
        return !isEmpty();
    }

    /**
     * Checks if the WireIn is empty.
     *
     * @return true if empty, otherwise false.
     */
    default boolean isEmpty() {
        return bytes().isEmpty();
    }

    /**
     * Adjusts the read position of the WireIn to align with the specified boundary.
     * Typically used for cases where data is structured in blocks of fixed sizes, ensuring proper alignment.
     *
     * @param alignment The byte boundary to which the read position should align.
     * @return the WireIn instance for method chaining.
     */
    @NotNull
    default WireIn readAlignTo(int alignment) {
        return this;
    }

    // TODO add a try-with-resource support for readDocument.

    /**
     * Reads a document, consuming both its metadata and data sections.
     *
     * @param metaDataConsumer Consumer that processes the metadata section of the document.
     * @param dataConsumer     Consumer that processes the main data section of the document.
     * @return true if the document was successfully read, otherwise false.
     * @throws InvalidMarshallableException if there's an error during marshalling.
     */
    default boolean readDocument(@Nullable ReadMarshallable metaDataConsumer,
                                 @Nullable ReadMarshallable dataConsumer) throws InvalidMarshallableException {
        return WireInternal.readData(this, metaDataConsumer, dataConsumer);
    }

    /**
     * Reads a document from a specific position, consuming both its metadata and data sections.
     *
     * @param position         The position from which to start reading the document.
     * @param metaDataConsumer Consumer that processes the metadata section of the document.
     * @param dataConsumer     Consumer that processes the main data section of the document.
     * @return true if the document was successfully read from the given position, otherwise false.
     * @throws InvalidMarshallableException if there's an error during marshalling.
     */
    default boolean readDocument(long position,
                                 @Nullable ReadMarshallable metaDataConsumer,
                                 @Nullable ReadMarshallable dataConsumer) throws InvalidMarshallableException {
        return WireInternal.readData(position, this, metaDataConsumer, dataConsumer);
    }

    /**
     * Performs a raw data read operation.
     *
     * @param marshallable Data to be read in its raw form.
     * @throws InvalidMarshallableException if there's an error during marshalling.
     */
    default void rawReadData(@NotNull ReadMarshallable marshallable) throws InvalidMarshallableException {
        WireInternal.rawReadData(this, marshallable);
    }

    /**
     * equivalent to {@link  WireIn#readDocument(net.openhft.chronicle.wire.ReadMarshallable,
     * net.openhft.chronicle.wire.ReadMarshallable)} but with out the use of a lambda expression
     *
     * @return the document context
     */
    @Override
    @NotNull
    DocumentContext readingDocument();

    /**
     * Provides a context for reading a document starting at a specific position.
     *
     * @param readLocation The position from which to start reading the document.
     * @return the document context associated with the specific read location.
     */
    DocumentContext readingDocument(long readLocation);

    /**
     * Consumes and discards any padding that may exist between the current read position and the next piece of meaningful data.
     */
    void consumePadding();

    /**
     * Registers a consumer to receive any comments encountered while parsing the
     * wire.  Comments are primarily used for debugging or additional metadata and
     * are not part of the logical data model.
     */
    void commentListener(Consumer<CharSequence> commentListener);

    /**
     * Consume a header if one is available.  This is typically used by queue or
     * networking layers to determine whether a complete message is ready to be
     * read.
     *
     * @return {@code true} if a data header was consumed
     */
    default boolean readDataHeader() throws EOFException {
        return readDataHeader(false) == HeaderType.DATA;
    }

    /**
     * Low level method used to read the next message header.  The returned
     * {@link HeaderType} indicates whether the message is data, metadata or the
     * end-of-stream marker.
     */
    @NotNull
    HeaderType readDataHeader(boolean includeMetaData) throws EOFException;

    /**
     * Reads and sets the length of the data or metadata at the specified position.
     *
     * @param position The position from which to start reading the length.
     */
    void readAndSetLength(long position);

    /**
     * Reads the first header in the stream with a timeout.
     *
     * @param timeout   Maximum time to wait for a header to be available.
     * @param timeUnit  The unit of time for the timeout.
     * @throws TimeoutException If no header is read within the given timeout.
     * @throws StreamCorruptedException If there is an error in reading the header due to stream corruption.
     */
    void readFirstHeader(long timeout, TimeUnit timeUnit) throws TimeoutException, StreamCorruptedException;

    /**
     * Reads the first header in the stream.
     *
     * @throws StreamCorruptedException If there is an error in reading the header due to stream corruption.
     */
    void readFirstHeader() throws StreamCorruptedException;

    /**
     * Reads the metadata header from the current position in the stream.
     */
    void readMetaDataHeader();

    /**
     * Returns a YAML representation of the remaining bytes without advancing the
     * read position.  Useful for logging and diagnostics.
     */
    String readingPeekYaml();

    /**
     * Marks the start of an event or method flow.
     */
    default void startEvent() {
    }

    /**
     * Checks if the current position in the stream represents the end of an event or method flow.
     *
     * @return true if the current position is the end of an event, otherwise false.
     */
    default boolean isEndEvent() {
        return false;
    }

    /**
     * Marks the end of an event or method flow.
     */
    default void endEvent() {
    }

    /**
     * Provides a hint about the order in which the input should be read.
     *
     * @return true if the input should be read in a specific order, otherwise false.
     */
    default boolean hintReadInputOrder() {
        return false;
    }

    /**
     * Checks if the current position in the stream has a metadata prefix.
     *
     * @return true if there's a metadata prefix at the current position, otherwise false.
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
