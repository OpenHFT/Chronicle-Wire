/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.EOFException;
import java.io.ObjectInput;
import java.io.StreamCorruptedException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The defines the stand interface for writing and reading sequentially to/from a Bytes stream.
 */
@DontChain
public interface WireIn extends WireCommon, MarshallableIn {

    @NotNull
    default <K, V> Map<K, V> readAllAsMap(Class<K> kClass, @NotNull Class<V> vClass, @NotNull Map<K, V> map) {
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

    void copyTo(@NotNull WireOut wire);

    /**
     * Read the field if present, or empty string if not present.
     */
    @NotNull
    ValueIn read();

    /**
     * Read the field if present which must match the WireKey.
     */
    @NotNull
    ValueIn read(@NotNull WireKey key);

    @NotNull
    default ValueIn read(String fieldName) {
        return read(() -> fieldName);
    }

    /**
     * @return field number or Long.MIN_VALUE if no number.
     */
    long readEventNumber();

    /**
     * Read a field, or string which is always written, even for formats which might drop the field
     * such as RAW.
     */
    @NotNull
    default ValueIn readEventName(@NotNull StringBuilder name) {
        try {
            return read(name);
        } catch (Exception e) {
            throw new IORuntimeException("failed to parse bytes=" + bytes().toDebugString(128), e);
        }
    }

    /**
     * Read the field if present, or empty string if not present.
     */
    @NotNull
    ValueIn read(@NotNull StringBuilder name);

    /**
     * Read a field which might be an object of any type.
     *
     * @param expectedClass to use as a hint, or Object.class if no hint available.
     * @return an instance of expectedClass
     */
    @Nullable <K> K readEvent(Class<K> expectedClass);

    /**
     * Obtain the value in (for internal use)
     */
    @NotNull
    ValueIn getValueIn();

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

    @NotNull
    default WireIn readAlignTo(int alignment) {
        return this;
    }

    // TODO add a try-with-resource support for readDocument.

    default boolean readDocument(@Nullable ReadMarshallable metaDataConsumer,
                                 @Nullable ReadMarshallable dataConsumer) {
        return WireInternal.readData(this, metaDataConsumer, dataConsumer);
    }

    default boolean readDocument(long position,
                                 @Nullable ReadMarshallable metaDataConsumer,
                                 @Nullable ReadMarshallable dataConsumer) {
        return WireInternal.readData(position, this, metaDataConsumer, dataConsumer);
    }

    default void rawReadData(@NotNull ReadMarshallable marshallable) {
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

    @Nullable
    default CharSequence asText() {
        return Wires.asText(this);
    }

    String readingPeekYaml();

    // Use for processing events/method flows.
    default void startEvent() {
    }

    default boolean isEndEvent() {
        return false;
    }

    default void endEvent() {
    }

    default boolean hintReadInputOrder() {
        return false;
    }

    enum HeaderType {
        NONE, DATA, META_DATA, EOF
    }
}
