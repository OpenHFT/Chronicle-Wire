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
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.annotation.DontChain;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.jetbrains.annotations.NotNull;

import java.io.ObjectOutput;
import java.io.StreamCorruptedException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * The defines the standard interface for writing and reading sequentially to/from a Bytes stream
 */
@DontChain
public interface WireOut extends WireCommon, MarshallableOut {
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
    /**
     * Write an event name. The event key can be of type WireKey or CharSequence.
     * If it doesn't match any of these types, an attempt will be made to write it as a
     * generic object. This method is mostly for internal use.
     *
     * @param expectedType The expected class of the event key
     * @param eventKey The event key to be written
     * @return An instance of ValueOut, which is used for further writing operations
     * @throws InvalidMarshallableException if the eventKey cannot be written
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    default ValueOut writeEvent(Class expectedType, Object eventKey) throws InvalidMarshallableException {
        if (eventKey instanceof WireKey)
            return writeEventName((WireKey) eventKey);
        if (eventKey instanceof CharSequence)
            return writeEventName((CharSequence) eventKey);
        writeStartEvent();
        getValueOut().object(expectedType, eventKey);
        writeEndEvent();
        return getValueOut();
    }

    /**
     * Write an event id. This method constructs a MethodWireKey with the given id and
     * passes it to the write method.
     *
     * @param methodId The id of the method to be written
     * @return An instance of ValueOut, which is used for further writing operations
     */
    default ValueOut writeEventId(int methodId) {
        return write(new MethodWireKey(null, methodId));
    }

    /**
     * Write an event id. This method constructs a MethodWireKey with the given name and id
     * and passes it to the write method.
     *
     * @param name The name of the method to be written
     * @param methodId The id of the method to be written
     * @return An instance of ValueOut, which is used for further writing operations
     */
    default ValueOut writeEventId(String name, int methodId) {
        return write(new MethodWireKey(name, methodId));
    }

    /**
     * Get the current ValueOut instance. The ValueOut interface is used to write values
     * of various types to the wire.
     *
     * @return The current ValueOut instance
     */
    @NotNull
    ValueOut write(WireKey key);

    ValueOut write(CharSequence key);

    /**
     * Obtain the value out
     */
    @NotNull
    ValueOut getValueOut();

    /**
     * Get the current ObjectOutput instance. The ObjectOutput interface is used for
     * writing objects to an underlying output stream.
     *
     * @return The current ObjectOutput instance
     */
    ObjectOutput objectOutput();

    /**
     * Write a comment. This method allows for writing comments to the wire.
     *
     * @param s The comment to be written
     * @return The current WireOut instance
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
    @NotNull
    default WireOut padToCacheAlign() {
        @NotNull Bytes<?> bytes = bytes();

        long offset = bytes.writePosition();
        if (bytes.start() != 0)
            offset = bytes.addressForRead(offset);
        int mod = (int) (offset & 63);
        if (mod > 60)
            addPadding(64 - mod);

        return this;
    }

    /**
     * Align the write position to a specified boundary. This method pads the write
     * position to a specified alignment boundary.
     *
     * @param alignment The alignment boundary
     * @param plus The additional number of bytes to be written
     * @return The current WireOut instance
     */
    @NotNull
    default WireOut writeAlignTo(int alignment, int plus) {
        assert Integer.bitCount(alignment) == 1;
        long mod = (bytes().writePosition() + plus) & (alignment - 1);
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
     */
    default void writeDocument(boolean metaData, @NotNull WriteMarshallable writer) throws InvalidMarshallableException {
        WireInternal.writeData(this, metaData, false, writer);
    }

    /**
     * This will increment the headerNumber as appropriate if successful
     *
     * @param metaData if {@code true} the document context will be used for writing meta data,
     *                 otherwise data
     * @return a document context used for witting
     */
    @Override
    DocumentContext writingDocument(boolean metaData);

    @Override
    @NotNull
    default DocumentContext writingDocument() {
        return writingDocument(false);
    }
    /**
     * Acquire the current writing DocumentContext. This method is mostly for internal use.
     *
     * @param metaData The metadata for the writing document
     * @return The current writing DocumentContext
     */
    DocumentContext acquireWritingDocument(boolean metaData);

    /**
     * This will increment the headerNumber as appropriate if successful
     * <p/>
     * This is used in networking, but no longer used in queue.
     *
     * @param metaData {@code true} if the write should write metaData rather than data
     * @param writer   writes bytes to the wire
     * @throws InvalidMarshallableException If the document could not be written
     */
    default void writeNotCompleteDocument(boolean metaData, @NotNull WriteMarshallable writer) throws InvalidMarshallableException {
        WireInternal.writeData(this, metaData, true, writer);
    }

    /**
     * INTERNAL METHOD, call writingDocument instead
     * <p>
     *
     * @param position The position of the header
     * @param metaData The metadata for the document
     * @param expectedHeader The expected header of the document
     * @throws StreamCorruptedException If the stream is corrupted
     */
    void updateHeader(long position, boolean metaData, int expectedHeader) throws StreamCorruptedException;

    /**
     * INTERNAL METHOD, call writingDocument instead
     * <p>
     * Start a header for a document
     * @param safeLength ensure there is at least this much space
     * @return the position of the header
     * @throws WriteAfterEOFException if you attempt to append an excerpt after an EOF has been written
     */
    long enterHeader(long safeLength);

    /**
     * INTERNAL METHOD, call writingDocument instead
     * <p>
     * Start the first header, if there is none This will increment the headerNumber as appropriate
     * if successful <p> Note: the file might contain other data and the caller has to check this.
     * </p>
     *
     * @return true if the header needs to be written, false if there is a data already
     */
    boolean writeFirstHeader();

    /**
     * INTERNAL METHOD, call writingDocument instead
     * <p>
     * Update the first header of a document after writing. This method is mostly for internal use.
     */
    void updateFirstHeader();

    /**
     * INTERNAL METHOD, call writingDocument instead
     * <p>
     * Update the first header of a document after writing a certain amount of bytes.
     * This method is mostly for internal use.
     *
     * @param headerLen The length of the header in bytes
     */
    void updateFirstHeader(long headerLen);

    /**
     * Write the end of wire marker, unless one is already written. This will increment the
     * headerNumber as appropriate if successful
     *
     * @param timeout      throw TimeoutException if it could not write the marker in time.
     * @param timeUnit     of the timeout
     * @param lastPosition the end of the wire
     * @return {code true} if did this method wrote EOF, {@code false} if it was already there.
     */
    boolean writeEndOfWire(long timeout, TimeUnit timeUnit, long lastPosition);

    /**
     * Check if end of wire marker is present, optionally writing it unless one is already written.
     * This will increment the headerNumber as appropriate if successful
     *
     * @param writeEOF     if {@code true}, write end of wire marker unless already exists
     * @param timeout      throw TimeoutException if it could not write the marker in time.
     * @param timeUnit     of the timeout
     * @param lastPosition the end of the wire
     * @return {@link EndOfWire} enum corresponding to EOF presence
     */
    default EndOfWire endOfWire(boolean writeEOF, long timeout, TimeUnit timeUnit, long lastPosition) {
        throw new UnsupportedOperationException("Optional operation, please use writeEndOfWire");
    }

    /**
     * Start an event object, mostly for internal use.
     */
    void writeStartEvent();

    /**
     * End an event object, mostly for internal use.
     */
    void writeEndEvent();

    /**
     * Write a map as series of events. Each entry is written as a separate event.
     *
     * @param kClass The class of the map keys
     * @param vClass The class of the map values
     * @param map The map to be written
     */
    default <K, V> void writeAllAsMap(Class<K> kClass, Class<V> vClass, @NotNull Map<K, V> map) {
        map.forEach((k, v) -> writeEvent(kClass, k).object(vClass, v));
    }

    /**
     * Specify whether default values should be dropped when writing. This is a no-op in this default
     * implementation but can be overridden in specific implementations.
     *
     * @param dropDefault Whether to drop default values when writing
     * @return The current WireOut instance
     */
    @NotNull
    default WireOut dropDefault(boolean dropDefault) {
        return this;
    }

    /**
     * Check if writing is complete. This is a no-op in this default implementation but can be overridden
     * in specific implementations.
     *
     * @return true unless there is an incomplete/chained message
     */
    default boolean writingIsComplete() {
        return true;
    }

    enum EndOfWire {
        /** EOF marker is not present and was not written */
        NOT_PRESENT,
        /** EOF marker was not present have been written and now in place */
        PRESENT_AFTER_UPDATE,
        /** EOF marker is present */
        PRESENT;
    }
}
