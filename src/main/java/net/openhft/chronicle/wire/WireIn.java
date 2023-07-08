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
 * This interface defines standard methods for sequential reading and writing to/from a Bytes stream.
 */
@DontChain
public interface WireIn extends WireCommon, MarshallableIn {

    /**
     * Reads all entries from the wire and populates the provided map with them.
     *
     * @param kClass the class of the key
     * @param vClass the class of the value
     * @param map    the map to be populated with entries read from the wire
     * @return the populated map
     * @throws InvalidMarshallableException if an object cannot be unmarshalled from the wire
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
     * Copies all remaining bytes from this wire to another.
     *
     * @param wire the wire to copy to
     * @throws InvalidMarshallableException if an object cannot be unmarshalled from the wire
     */
    void copyTo(@NotNull WireOut wire) throws InvalidMarshallableException;

    /**
     * Reads a field from the wire if it's present, or returns an empty string if it's not.
     */
    @NotNull
    ValueIn read();

    /**
     * Read the field if present which must match the WireKey.
     */
    @NotNull
    ValueIn read(@NotNull WireKey key);

    /**
     * Reads a field using the provided string as the field name.
     *
     * @param fieldName the name of the field to be read
     * @return the value contained in the field
     */
    @NotNull
    default ValueIn read(String fieldName) {
        return read(() -> fieldName);
    }

    /**
     * Retrieves the number associated with the current event field or Long.MIN_VALUE if there isn't any.
     *
     * @return the event number or Long.MIN_VALUE if no number.
     */
    long readEventNumber();

    /**
     * Read a field, or string which is always written, even for formats which might drop the field
     * such as RAW.
     *
     * @param name the StringBuilder to hold the field name
     * @return the value associated with the field
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
     * Read the field if present, or empty string if not present.
     */
    @NotNull
    ValueIn read(@NotNull StringBuilder name);

    /**
     * Read a field which might be an object of any type.
     * <p>
     * Use getValueIn() to read the value for this event
     *
     * @param expectedClass to use as a hint, or Object.class if no hint available.
     * @return an instance of expectedClass
     */
    @Nullable <K> K readEvent(Class<K> expectedClass) throws InvalidMarshallableException;

    /**
     * Obtain the value in for advanced use (typically after a call to readEvent above)
     */
    @NotNull
    ValueIn getValueIn();

    /**
     * Provides a wrapper around an input object stream.
     *
     * @return an ObjectInput instance associated with the wire
     */
    ObjectInput objectInput();

    /*
     * read and write comments.
     */
    @NotNull
    WireIn readComment(@NotNull StringBuilder sb);

    @Override
    void clear();

    /**
     * This consumes any padding before checking if readRemaining() &gt; 0 <p> NOTE: This method
     * only works inside a document. Call it just before a document and it won't know not to read
     * the read in case there is padding.
     *
     * @return if there is more data to be read in this document.
     */
    default boolean hasMore() {
        return isNotEmptyAfterPadding();
    }

    /**
     * This consumes any padding before checking if readRemaining() &gt; 0 <p> NOTE: This method
     * only works inside a document. Call it just before a document and it won't know not to read
     * the read in case there is padding.
     *
     * @return if there is more data to be read in this document.
     */
    default boolean isNotEmptyAfterPadding() {
        consumePadding();
        return !isEmpty();
    }

    default boolean isEmpty() {
        return bytes().isEmpty();
    }

    /**
     * Adjusts the read position of the underlying Bytes to be aligned to the specified alignment boundary.
     *
     * @param alignment the byte boundary to align the read position to
     * @return this wire
     */
    @NotNull
    default WireIn readAlignTo(int alignment) {
        return this;
    }

    // TODO add a try-with-resource support for readDocument.

    default boolean readDocument(@Nullable ReadMarshallable metaDataConsumer,
                                 @Nullable ReadMarshallable dataConsumer) throws InvalidMarshallableException {
        return WireInternal.readData(this, metaDataConsumer, dataConsumer);
    }

    default boolean readDocument(long position,
                                 @Nullable ReadMarshallable metaDataConsumer,
                                 @Nullable ReadMarshallable dataConsumer) throws InvalidMarshallableException {
        return WireInternal.readData(position, this, metaDataConsumer, dataConsumer);
    }

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

    DocumentContext readingDocument(long readLocation);

    void consumePadding();

    /**
     * Sets a listener that will be notified when a comment is read from the wire.
     *
     * @param commentListener the listener to be notified of comments
     */
    void commentListener(Consumer<CharSequence> commentListener);

    /**
     * Consume a header if one is available.
     *
     * @return true, if a message can be read between readPosition and readLimit, else false if no
     * header is ready.
     * @throws EOFException if the end of wire marker is reached.
     */
    default boolean readDataHeader() throws EOFException {
        return readDataHeader(false) == HeaderType.DATA;
    }

    @NotNull
    HeaderType readDataHeader(boolean includeMetaData) throws EOFException;

    void readAndSetLength(long position);

    void readFirstHeader(long timeout, TimeUnit timeUnit) throws TimeoutException, StreamCorruptedException;

    void readFirstHeader() throws StreamCorruptedException;

    void readMetaDataHeader();

    @Deprecated(/* to be removed in x.25 */)
    @Nullable
    default CharSequence asText() {
        return Wires.asText(this);
    }

    /**
     * Returns a peek of the data in the wire in YAML format.
     *
     * @return a string representation of the data in the wire
     */
    String readingPeekYaml();

    /**
     * Called to signal the start of an event when processing events or method flows.
     */
    default void startEvent() {
    }

    /**
     * Checks if the end of an event has been reached when processing events or method flows.
     *
     * @return true if the end of the event has been reached, false otherwise
     */
    default boolean isEndEvent() {
        return false;
    }

    /**
     * Called to signal the end of an event when processing events or method flows.
     */
    default void endEvent() {
    }

    /**
     * Provides a hint on whether the read operations should follow the order of the input data.
     *
     * @return true if the order of the input data should be followed, false otherwise
     */
    default boolean hintReadInputOrder() {
        return false;
    }

    /**
     * Checks if the wire has a metadata prefix.
     *
     * @return true if the wire has a metadata prefix, false otherwise
     */
    default boolean hasMetaDataPrefix() {
        return false;
    }
    /**
     * Enum indicating the type of header encountered in the wire.
     */
    enum HeaderType {
        NONE, DATA, META_DATA, EOF
    }
}
