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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.annotation.DontChain;
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    default ValueOut writeEvent(Class expectedType, Object eventKey) {
        if (eventKey instanceof WireKey)
            return writeEventName((WireKey) eventKey);
        if (eventKey instanceof CharSequence)
            return writeEventName((CharSequence) eventKey);
        writeStartEvent();
        getValueOut().object(expectedType, eventKey);
        writeEndEvent();
        return getValueOut();
    }

    default ValueOut writeEventId(int methodId) {
        return write(new MethodWireKey(null, methodId));
    }

    default ValueOut writeEventId(String name, int methodId) {
        return write(new MethodWireKey(name, methodId));
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
    @NotNull
    default WireOut padToCacheAlign() {
        @NotNull Bytes<?> bytes = bytes();
        try {
            long offset = bytes.writePosition();
            if (bytes.start() != 0)
                offset = bytes.addressForRead(offset);
            int mod = (int) (offset & 63);
            if (mod > 60)
                addPadding(64 - mod);
        } catch (IllegalArgumentException ignored) {
        }
        return this;
    }

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
    @Override
    DocumentContext writingDocument(boolean metaData);

    @Override
    @NotNull
    default DocumentContext writingDocument() {
        return writingDocument(false);
    }

    DocumentContext acquireWritingDocument(boolean metaData);

    /**
     * This will increment the headerNumber as appropriate if successful
     * <p/>
     * This is used in networking, but no longer used in queue.
     *
     * @param metaData {@code true} if the write should write metaData rather than data
     * @param writer   writes bytes to the wire
     */
    default void writeNotCompleteDocument(boolean metaData, @NotNull WriteMarshallable writer) {
        WireInternal.writeData(this, metaData, true, writer);
    }

    void updateHeader(long position, boolean metaData, int expectedHeader) throws StreamCorruptedException;

    @Deprecated(/* to be removed in x.23*/)
    default long enterHeader(int safeLength) {
        return enterHeader((long) safeLength);
    }

    long enterHeader(long safeLength);

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
     * @return did this method write EOF or was it already there.
     */
    boolean writeEndOfWire(long timeout, TimeUnit timeUnit, long lastPosition);

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
}
