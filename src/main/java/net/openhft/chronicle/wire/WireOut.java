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
 * Defines the standard interface for sequential writing to a {@link Bytes}
 * stream.
 * <p>
 * Typical use is to obtain a {@link DocumentContext} and then write fields in
 * sequence.
 *
 * <pre>{@code
 * try (DocumentContext dc = wire.writingDocument()) {
 *     wire.writeEventName("price").float64(42.5);
 * }
 * }</pre>
 */
@DontChain
public interface WireOut extends WireCommon, MarshallableOut {
    /**
     * Writes an empty field marker or prepares for a value without an explicit
     * key, depending on the wire format.
     *
     * @return interface used to serialise the value
     */
    @NotNull
    ValueOut write();

    /**
     * Writes a field name or event key and prepares for the following value.
     * Usually equivalent to {@link #write(WireKey)}.
     *
     * @param key name of the field/event
     * @return interface used to serialise the value
     */
    @NotNull
    default ValueOut writeEventName(WireKey key) {
        return write(key);
    }

    /**
     * Writes a textual field name or event key and prepares for the following
     * value.
     *
     * @param key name of the field/event
     * @return interface used to serialise the value
     */
    default ValueOut writeEventName(CharSequence key) {
        return write(key);
    }

    /**
     * Serialises an event where the key may be an {@code enum} or arbitrary
     * object. The {@code expectedType} hints how the key should be written.
     *
     * @param keyType expected class of {@code key}
     * @param key     the key object to serialise
     * @return interface used to serialise the value
     * @throws InvalidMarshallableException if the key cannot be written
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    default ValueOut writeEvent(Class<?> keyType, Object key) throws InvalidMarshallableException {
        if (key instanceof WireKey)
            return writeEventName((WireKey) key);
        if (key instanceof CharSequence)
            return writeEventName((CharSequence) key);
        writeStartEvent();
        getValueOut().object(keyType, key);
        writeEndEvent();
        return getValueOut();
    }

    /**
     * Writes a numeric event identifier for compact binary formats.
     *
     * @param eventId numeric id representing the method/event
     * @return interface used to serialise the value
     */
    default ValueOut writeEventId(int eventId) {
        return write(new MethodWireKey(null, eventId));
    }

    /**
     * Writes a numeric event identifier and optional textual name. Useful for
     * debugging or human readable logs.
     *
     * @param eventName event name for diagnostic output
     * @param eventId numeric id representing the method/event
     * @return interface used to serialise the value
     */
    default ValueOut writeEventId(String eventName, int eventId) {
        return write(new MethodWireKey(eventName, eventId));
    }

    /**
     * Writes a field name using a {@link WireKey} and returns a
     * {@link ValueOut} for the associated value.
     *
     * @param key field identifier
     * @return interface used to serialise the value
     */
    @NotNull
    ValueOut write(WireKey key);

    /**
     * Writes a textual field name and returns a {@link ValueOut} for the
     * associated value.
     *
     * @param fieldName field identifier
     * @return interface used to serialise the value
     */
    ValueOut write(CharSequence fieldName);

    /**
     * Retrieves the interface for defining the output of a value
     * that will be written to the stream.
     *
     * @return The interface to further define the output for the written value.
     */
    @NotNull
    ValueOut getValueOut();

    /**
     * Provides a standard {@link ObjectOutput} view over this wire.
     * Useful when integrating with APIs expecting the Java serialisation API.
     *
     * @return object output facade
     */
    ObjectOutput objectOutput();

    /**
     * Writes a comment to the wire. Comments may be useful for debugging
     * or providing context within the wire stream.
     *
     * @param s The comment to be written to the stream.
     * @return This WireOut instance for method chaining.
     */
    @NotNull
    WireOut writeComment(CharSequence s);

    /**
     * Inserts the given number of pad bytes. Often used to reserve space or
     * align the next field.
     *
     * @param paddingToAdd number of zero bytes to write
     * @return this instance for chaining
     */
    @NotNull
    WireOut addPadding(int paddingToAdd);

    /**
     * Pads to the next cache line so that a small header does not straddle
     * cache boundaries and cause false sharing.
     *
     * @return this instance for chaining
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
     * Aligns the write position to the specified power of two. Padding may be
     * inserted so that {@code writePosition()+plus} becomes a multiple of
     * {@code alignment}.
     *
     * @param alignment alignment boundary in bytes (power of two)
     * @param plus      additional offset
     * @return this instance for chaining
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
     * Convenience wrapper around {@link #writingDocument(boolean)} for one off
     * writes. The document is closed automatically.
     *
     * @param isMetaData write metadata rather than data
     * @param bodyWriter   callback used to write the document body
     */
    default void writeDocument(boolean isMetaData, @NotNull WriteMarshallable bodyWriter) throws InvalidMarshallableException {
        WireInternal.writeData(this, isMetaData, false, bodyWriter);
    }

    /**
     * Begin writing a document. The returned context must be closed, typically
     * via a try-with-resources block.
     *
     * @param metaData if {@code true} a metadata document is started
     * @return context controlling the document write
     */
    @Override
    DocumentContext writingDocument(boolean metaData);

    /**
     * Shortcut for {@code writingDocument(false)}.
     *
     * @return context controlling the document write
     */
    @Override
    @NotNull
    default DocumentContext writingDocument() {
        return writingDocument(false);
    }

    /**
     * Acquire a document context, reusing any that may already be active.
     * Useful when writing multiple documents in a chain.
     *
     * @param includeMetaData if {@code true} a metadata document is required
     * @return context controlling the document write
     */
    DocumentContext acquireWritingDocument(boolean includeMetaData);

    /**
     * Write a document without the usual completion marker. Originally used for
     * streaming over TCP and seldom required now.
     *
     * @param isMetaData write metadata rather than data
     * @param writer   callback used to write the document body
     */
    default void writeNotCompleteDocument(boolean isMetaData, @NotNull WriteMarshallable writer) throws InvalidMarshallableException {
        WireInternal.writeData(this, isMetaData, true, writer);
    }

    /**
     * INTERNAL METHOD used by {@link DocumentContext}. Updates the header when
     * a document is completed.
     */
    void updateHeader(long headerPos, boolean isMetaData, int templateHeader) throws StreamCorruptedException;

    /**
     * INTERNAL METHOD used by {@link DocumentContext}. Begins a document
     * header and reserves space for it.
     *
     * @param safeLength ensure there is at least this much space
     * @return position of the header
     * @throws WriteAfterEOFException if appending after EOF
     */
    long enterHeader(long safeLength);

    /**
     * INTERNAL METHOD. Called once to create the first header if required.
     * The caller must ensure the file is empty.
     *
     * @return {@code true} if a header was written
     */
    boolean writeFirstHeader();

    /** INTERNAL METHOD used by DocumentContext to update the first header. */
    void updateFirstHeader();

    /** INTERNAL METHOD used by DocumentContext to update the first header. */
    void updateFirstHeader(long headerLen);

    /**
     * Writes an end of wire marker if one is not already present. Used by
     * queue implementations to signal the last message.
     *
     * @param timeout      maximum time to wait
     * @param timeoutUnit  unit of {@code timeout}
     * @param lastPosition position considered to be the end of the wire
     * @return {@code true} if a marker was written
     */
    boolean writeEndOfWire(long timeout, TimeUnit timeoutUnit, long lastPosition);

    /**
     * Tests for the end of wire marker and optionally writes one. Mainly used
     * by persisted queues.
     *
     * @param writeEOF     if {@code true} write marker when missing
     * @param timeout      maximum time to wait
     * @param timeUnit     unit of {@code timeout}
     * @param lastPosition position considered to be the end of the wire
     * @return {@link EndOfWire} describing the marker status
     */
    default EndOfWire endOfWire(boolean shouldWriteEof, long timeout, TimeUnit timeoutUnit, long lastPosition) {
        throw new UnsupportedOperationException("Optional operation, please use writeEndOfWire");
    }

    /**
     * INTERNAL: mark the start of a structured event such as a map entry.
     */
    void writeStartEvent();

    /** INTERNAL: mark the end of a structured event. */
    void writeEndEvent();

    /**
     * Convenience for emitting all map entries as {@code key:value} pairs.
     */
    default <K, V> void writeAllAsMap(Class<K> kClass, Class<V> vClass, @NotNull Map<K, V> map) {
        map.forEach((k, v) -> writeEvent(kClass, k).object(vClass, v));
    }

    /**
     * Control whether fields with default values are omitted from output.
     */
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
