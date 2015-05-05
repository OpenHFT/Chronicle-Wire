/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;


import net.openhft.chronicle.bytes.Bytes;

import java.util.function.Consumer;

/**
 * The defines the stand interface for writing and reading sequentially to/from a Bytes stream.
 * <p>
 * Created by peter.lawrey on 12/01/15.
 */
public interface WireOut {

    /**
     * Write an empty filed marker
     */
    ValueOut write();

    /**
     * Always write a key.  For RAW types, this label with be in text.  To read this, use readEventName()
     */
    default ValueOut writeEventName(WireKey key) {
        return write(key);
    }

    /**
     * Write a key for wires that support fields.
     */
    ValueOut write(WireKey key);

    /**
     * write a field less value.
     */
    ValueOut writeValue();

    /**
     * Obtain the value out (for internal use)
     */
    ValueOut getValueOut();

    /*
     * read and write comments.
     */
    WireOut writeComment(CharSequence s);

    boolean hasDocument();

    Bytes bytes();

    WireOut addPadding(int paddingToAdd);

    default void writeDocument(boolean metaData, Consumer<WireOut> writer) {
        Wires.writeData(this, metaData, writer);
    }

    WriteMarshallable EMPTY = wire -> {
        // nothing
    };
}
