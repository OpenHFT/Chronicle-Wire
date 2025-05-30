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
import net.openhft.chronicle.core.annotation.DontChain;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.jetbrains.annotations.NotNull;

import java.io.ObjectOutput;
import java.io.StreamCorruptedException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Defines the standard interface for sequential writing to a Bytes stream.
 */
@DontChain
public interface WireOut extends WireCommon, MarshallableOut {
    /**
     * Writes a field marker with no name. For sequence items the returned
     * {@link ValueOut} is used to write the value.
     */
    @NotNull
    ValueOut write();

    /**
     * Writes an event key to the stream. RAW wires emit it in text so that
     * {@link WireIn#readEventName(StringBuilder)} can later match it.
     */
    @NotNull
    default ValueOut writeEventName(WireKey key) {
        return write(key);
    }

    /**
     * Writes an event key from arbitrary text.
     */
    default ValueOut writeEventName(CharSequence key) {
        return write(key);
    }

    /**
     * Writes an event key whose type is not a simple {@link WireKey}.
     * The {@code expectedType} hints how the key should be serialised.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    default ValueOut writeEvent(Class<?> expectedType, Object eventKey) throws InvalidMarshallableException {
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
     * Writes a numeric event identifier for binary wires.
     */
    default ValueOut writeEventId(int methodId) {
        return write(new MethodWireKey(null, methodId));
    }

    /**
     * Writes a numeric identifier along with a textual name for debugging.
     */
    default ValueOut writeEventId(String name, int methodId) {
        return write(new MethodWireKey(name, methodId));
    }

    /**
     * Writes a structured key then returns a {@link ValueOut} for the value.
     */
    @NotNull
    ValueOut write(WireKey key);

    /**
     * Writes an arbitrary textual key.
     */
    ValueOut write(CharSequence key);

    /**
     * Retrieves the interface for defining the output of a value
     * that will be written to the stream.
     *
     * @return The interface to further define the output for the written value.
     */
    @NotNull
    ValueOut getValueOut();

    /**
     * Returns a standard {@link ObjectOutput} view over this wire.
     */
    ObjectOutput objectOutput();

    /**
     * Writes a comment for diagnostic dumps only.
     */
    @NotNull
    WireOut writeComment(CharSequence s);

    /**
     * Inserts padding bytes, typically for alignment.
     */
    @NotNull
    WireOut addPadding(int paddingToAdd);

    /**
     * Pads the output so the next four bytes do not straddle a cache line.
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
     * Aligns the write position to {@code alignment} bytes from the optional offset.
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
     * Resets both the positions in the wire and the header number.
     */
    @Override
    void clear();

    /**
     * Writes a document to the wire. This will automatically handle header numbering.
     *
     * @param metaData Indicates if metadata should be written.
     * @param writer The logic for writing the content of the document.
     */
    default void writeDocument(boolean metaData, @NotNull WriteMarshallable writer) throws InvalidMarshallableException {
        WireInternal.writeData(this, metaData, false, writer);
    }

    /**
     * Starts the process of writing a document to the wire with an option for metadata.
     *
     * @param metaData If true, the returned document context will be used for writing metadata.
     * @return A context for the document being written.
     */
    @Override
    DocumentContext writingDocument(boolean metaData);

    /**
     * Starts the process of writing a data document (not metadata) to the wire.
     *
     * @return A context for the document being written.
     */
    @Override
    @NotNull
    default DocumentContext writingDocument() {
        return writingDocument(false);
    }

    /**
     * Retrieves a context for writing either data or metadata, reusing an existing context if available.
     *
     * @param metaData If true, the returned context will be used for writing metadata.
     * @return A context for the document being written.
     */
    DocumentContext acquireWritingDocument(boolean metaData);

    /**
     * Writes a document to the wire without marking its completion. This is primarily used in
     * networking scenarios, but no longer used for queues.
     *
     * @param metaData If true, metadata will be written instead of regular data.
     * @param writer Logic for writing the content of the document.
     */
    default void writeNotCompleteDocument(boolean metaData, @NotNull WriteMarshallable writer) throws InvalidMarshallableException {
        WireInternal.writeData(this, metaData, true, writer);
    }

    /**
     * INTERNAL METHOD, call writingDocument instead
     * <p>
     * Update/end a header for a document
     */
    void updateHeader(long position, boolean metaData, int expectedHeader) throws StreamCorruptedException;

    /**
     * INTERNAL METHOD, call writingDocument instead
     * <p>
     * Start a header for a document
     *
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
     *
     * @return true if the header needs to be written, false if there is a data already
     */
    boolean writeFirstHeader();

    /**
     * INTERNAL METHOD, call writingDocument instead
     * <p>
     * update the first header after writing.
     */
    void updateFirstHeader();

    /**
     * INTERNAL METHOD, call writingDocument instead
     * <p>
     * update the first header after writing {@code headerEndPos} bytes.
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

    void writeEndEvent();

    default <K, V> void writeAllAsMap(Class<K> kClass, Class<V> vClass, @NotNull Map<K, V> map) {
        map.forEach((k, v) -> writeEvent(kClass, k).object(vClass, v));
    }

    @NotNull
    default WireOut dropDefault(boolean dropDefault) {
        return this;
    }

    /**
     * @return true unless there is an incomplete/chained message
     */
    default boolean writingIsComplete() {
        return true;
    }

    enum EndOfWire {
        /**
         * EOF marker is not present and was not written
         */
        NOT_PRESENT,
        /**
         * EOF marker was not present have been written and now in place
         */
        PRESENT_AFTER_UPDATE,
        /**
         * EOF marker is present
         */
        PRESENT
    }
}
