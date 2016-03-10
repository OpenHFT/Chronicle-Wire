/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.EOFException;
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
     * Obtain the value in (for internal use)
     */
    @NotNull
    ValueIn getValueIn();

    /*
     * read and write comments.
     */
    @NotNull
    Wire readComment(@NotNull StringBuilder sb);

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
    boolean readHeader() throws EOFException;

    void readFirstHeader(long timeout, TimeUnit timeUnit) throws TimeoutException, StreamCorruptedException;
}
