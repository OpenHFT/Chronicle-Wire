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

/**
 * The defines the stand interface for writing and reading sequentially to/from a Bytes stream.
 * <p>
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

    /**
     * Write a key for wires that support fields.
     */
    @NotNull
    ValueOut write(WireKey key);

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
}
