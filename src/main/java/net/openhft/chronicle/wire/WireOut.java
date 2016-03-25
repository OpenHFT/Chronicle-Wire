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

import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.StreamCorruptedException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The defines the stand interface for writing and reading sequentially to/from a Bytes stream.
 * <p/>
 * Created by peter.lawrey on 12/01/15.
 */
public interface WireOut extends WireCommon {

    /**
     * Write an empty filed marker
     */
    @NotNull
    ValueOut write();

    /**
     * Always write a key.  For RAW types, this label with be in text.  To read this, use readEventName()
     */
    @NotNull
    default ValueOut writeEventName(WireKey key) {
        return write(key);
    }

    default ValueOut writeEventName(CharSequence key) {
        return write(key);
    }

    /**
     * Write a key for wires that support fields.
     */
    @NotNull
    ValueOut write(WireKey key);

    ValueOut write(CharSequence key);

    /**
     * Obtain the value out
     */
    @NotNull
    ValueOut getValueOut();

    /*
     * read and write comments.
     */
    @NotNull
    WireOut writeComment(CharSequence s);

    @NotNull
    WireOut addPadding(int paddingToAdd);

    @NotNull
    default WireOut writeAlignTo(int alignment) {
        long mod = bytes().writePosition() % alignment;
        if (mod != 0)
            addPadding((int) (alignment - mod));
        return this;
    }

    default void writeDocument(boolean metaData, @NotNull WriteMarshallable writer) {
        WireInternal.writeData(this, metaData, false, writer);
    }

    DocumentContext writingDocument(boolean metaData);

    default void writeNotReadyDocument(boolean metaData, @NotNull WriteMarshallable writer) {
        WireInternal.writeData(this, metaData, true, writer);
    }

    /**
     * Write a new header, an unknown length, handling timeouts and the end of wire marker.
     *
     * @param timeout  throw a TimeoutException if the header could not be written in this time.
     * @param timeUnit of the timeOut
     * @return the position of the start of the header
     * @throws TimeoutException the underlying pauser timed out.
     * @throws EOFException     the end of wire marker was reached.
     */
    default long writeHeader(long timeout, TimeUnit timeUnit) throws TimeoutException, EOFException {
        return writeHeader(Wires.UNKNOWN_LENGTH, timeout, timeUnit);
    }

    /**
     * Change the header from NOT_READY | UNKNOWN_LENGTH to metaData * META_DATA | length.
     *
     * @param position returned by writeHeader
     * @param metaData whether the message is meta data or not.
     * @throws StreamCorruptedException
     */
    default void updateHeader(long position, boolean metaData) throws StreamCorruptedException {
        updateHeader(Wires.UNKNOWN_LENGTH, position, metaData);
    }

    /**
     * Write a message of a known length, handling timeouts and the end of wire marker.
     *
     * @param length   the maximum length of the message.
     * @param timeout  throw a TimeoutException if the header could not be written in this time.
     * @param timeUnit of the timeOut
     * @return the position of the start of the header
     * @throws TimeoutException the underlying pauser timed out.
     * @throws EOFException     the end of wire marker was reached.
     */
    long writeHeader(int length, long timeout, TimeUnit timeUnit) throws TimeoutException, EOFException;

    /**
     * Change the header from NOT_READY | length to metaData * META_DATA | length.
     *
     * @param length   provided to make the header, note this can be larger than the message actually used.
     * @param position returned by writeHeader
     * @param metaData whether the message is meta data or not.
     * @throws StreamCorruptedException
     */
    void updateHeader(int length, long position, boolean metaData) throws StreamCorruptedException;

    /**
     * Start the first header, if there is none
     * <p>
     * Note: the file might contain other data and the caller has to check this.
     * </p>
     *
     * @return true if the header needs to be written, false if there is a data already
     */
    boolean writeFirstHeader();

    /**
     * update the first header after writing.
     */
    void updateFirstHeader();

    /**
     * Write the end of wire marker, unless one is already written.
     *
     * @param timeout  throw TimeoutException if it could not write the marker in time.
     * @param timeUnit of the timeout
     * @throws TimeoutException timeout exceeded.
     */

    void writeEndOfWire(long timeout, TimeUnit timeUnit) throws TimeoutException;
}
