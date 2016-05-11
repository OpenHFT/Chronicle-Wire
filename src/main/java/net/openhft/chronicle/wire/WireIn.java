/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.EOFException;
import java.io.ObjectInput;
import java.io.StreamCorruptedException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The defines the stand interface for writing and reading sequentially to/from a Bytes stream. <p>
 * Created by peter.lawrey on 12/01/15.
 */
public interface WireIn extends WireCommon {

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
    <K> K readEvent(Class<K> expectedClass);

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

    void clear();

    /**
     * @return if there is more data to be read in this document.
     */
    boolean hasMore();

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

    default void rawReadData(ReadMarshallable marshallable) {
        WireInternal.rawReadData(this, marshallable);
    }

    /**
     * equivalent to {@link  WireIn#readDocument(net.openhft.chronicle.wire.ReadMarshallable,
     * net.openhft.chronicle.wire.ReadMarshallable)} but with out the use of a lambda expression
     *
     * @return the document context
     */
    DocumentContext readingDocument();

    DocumentContext readingDocument(long readLocation);

    void consumePadding();

    /**
     * Consume a header if one is available.
     *
     * @return true, if a message can be read between readPosition and readLimit, else false if no header is ready.
     * @throws EOFException if the end of wire marker is reached.
     */
    default boolean readDataHeader() throws EOFException {
        return readDataHeader(false) == HeaderType.DATA;
    }

    HeaderType readDataHeader(boolean includeMetaData) throws EOFException;

    void readAndSetLength(long position);

    void readFirstHeader(long timeout, TimeUnit timeUnit) throws TimeoutException, StreamCorruptedException;

    void readMetaDataHeader();

    default CharSequence asText() {
        return Wires.asText(this);
    }

    enum HeaderType {
        NONE, DATA, META_DATA
    }
}
