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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.EOFException;
import java.io.ObjectOutput;
import java.io.StreamCorruptedException;
import java.util.Map;
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
     * Always write a key.  For RAW types, this label with be in text.  To read this, use
     * readEventName()
     */
    @NotNull
    default ValueOut writeEventName(WireKey key) {
        return write(key);
    }

    default ValueOut writeEventName(CharSequence key) {
        return write(key);
    }

    default ValueOut writeEvent(Class expectedType, Object eventKey) {
        if (eventKey instanceof WireKey)
            return writeEventName((WireKey) eventKey);
        if (eventKey instanceof CharSequence)
            return writeEventName((CharSequence) eventKey);
        startEvent();
        getValueOut().object(expectedType, eventKey);
        endEvent();
        return getValueOut();
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

    ObjectOutput objectOutput();

    /*
     * read and write comments.
     */
    @NotNull
    WireOut writeComment(CharSequence s);

    @NotNull
    WireOut addPadding(int paddingToAdd);

    /**
     * If near the end of a cache line, pad it so a following 4-byte int value will not split a
     * cache line.
     *
     * @return this
     */
    default WireOut padToCacheAlign() {
        Bytes<?> bytes = bytes();
        int mod = (int) (bytes.address(bytes.writePosition()) & 63);
        if (mod > 60)
            addPadding(64 - mod);
        return this;
    }

    @NotNull
    default WireOut writeAlignTo(int alignment, int plus) {
        long mod = (bytes().writePosition() + plus) % alignment;
        if (mod != 0)
            addPadding((int) (alignment - mod));
        return this;
    }

    /**
     * This will reset the positions and the header number.
     */
    @Override
    void clear();

    /**
     * This will increment the headerNumber as appropriate if successful
     *
     * @param metaData
     * @param writer
     */
    default void writeDocument(boolean metaData, @NotNull WriteMarshallable writer) {
        WireInternal.writeData(this, metaData, false, writer);
    }

    /**
     * This will increment the headerNumber as appropriate if successful
     *
     * @param metaData if {@code true} the document context will be used for writing meta data,
     *                 otherwise data
     * @return a document context used for witting
     */
    DocumentContext writingDocument(boolean metaData);

    default DocumentContext writingDocument() {
        return writingDocument(false);
    }

    /**
     * This will increment the headerNumber as appropriate if successful
     *
     * @param metaData {@code true} if the write should write metaData rather than data
     * @param writer   writes bytes to the wire
     */
    default void writeNotCompleteDocument(boolean metaData, @NotNull WriteMarshallable writer) {
        WireInternal.writeData(this, metaData, true, writer);
    }

    /**
     * Write a new header, an unknown length, handling timeouts and the end of wire marker. This
     * will increment the headerNumber as appropriate if successful
     *
     * @param timeout      throw a TimeoutException if the header could not be written in this
     *                     time.
     * @param timeUnit     of the timeOut
     * @param lastPosition the last known position
     * @return the position of the start of the header
     * @throws TimeoutException the underlying pauser timed out.
     * @throws EOFException     the end of wire marker was reached.
     */
    default long writeHeader(long timeout, TimeUnit timeUnit, @Nullable final LongValue
            lastPosition) throws TimeoutException, EOFException {
        return writeHeader(Wires.UNKNOWN_LENGTH, timeout, timeUnit, lastPosition);
    }

    /**
     * Change the header from NOT_COMPLETE | UNKNOWN_LENGTH to metaData * META_DATA | length.
     *
     * @param position returned by writeHeader
     * @param metaData whether the message is meta data or not.
     * @throws StreamCorruptedException if the steam has become corrupted
     */
    default void updateHeader(long position, boolean metaData) throws StreamCorruptedException {
        updateHeader(Wires.UNKNOWN_LENGTH, position, metaData);
    }

    /**
     * Write a message of a known length, handling timeouts and the end of wire marker. This will
     * increment the headerNumber as appropriate if successful
     *
     * @param length       the maximum length of the message.
     * @param timeout      throw a TimeoutException if the header could not be written in this
     *                     time.
     * @param timeUnit     of the timeOut
     * @param lastPosition the last known position
     * @return the position of the start of the header
     * @throws TimeoutException the underlying pauser timed out.
     * @throws EOFException     the end of wire marker was reached.
     */
    long writeHeader(int length, long timeout, TimeUnit timeUnit, @Nullable LongValue lastPosition)
            throws TimeoutException, EOFException;

    /**
     * Change the header from NOT_COMPLETE | length to metaData * META_DATA | length.
     *
     * @param length   provided to make the header, note this can be larger than the message
     *                 actually used.
     * @param position returned by writeHeader
     * @param metaData whether the message is meta data or not.
     * @throws StreamCorruptedException if the steam has become corrupted
     */
    void updateHeader(int length, long position, boolean metaData) throws StreamCorruptedException;

    /**
     * Start the first header, if there is none This will increment the headerNumber as appropriate
     * if successful <p> Note: the file might contain other data and the caller has to check this.
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
     * Write the end of wire marker, unless one is already written. This will increment the
     * headerNumber as appropriate if successful
     *
     * @param timeout      throw TimeoutException if it could not write the marker in time.
     * @param timeUnit     of the timeout
     * @param lastPosition the end of the wire
     * @throws TimeoutException timeout exceeded.
     */

    void writeEndOfWire(long timeout, TimeUnit timeUnit, long lastPosition) throws TimeoutException;

    /**
     * Start an event object, mostly for internal use.
     */
    void startEvent();

    void endEvent();

    default <K, V> void writeAllAsMap(Class<K> kClass, Class<V> vClass, Map<K, V> map) {
        map.forEach((k, v) -> {
            writeEvent(kClass, k).object(vClass, v);
        });
    }

    default WireOut dropDefault(boolean dropDefault) {
        return this;
    }
}
