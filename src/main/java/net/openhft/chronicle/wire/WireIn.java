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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The defines the stand interface for writing and reading sequentially to/from a Bytes stream. <p> Created by peter.lawrey on
 * 12/01/15.
 */
public interface WireIn {
    boolean isReady();
    
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
     * Read a field, or string which is always written, even for formats which might drop the field such as RAW.
     */
    @NotNull
    default ValueIn readEventName(@NotNull StringBuilder name) {
        return read(name);
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

    void flip();

    void clear();

    Bytes<?> bytes();

    default boolean readDocument(@Nullable Consumer<WireIn> metaDataConsumer,
                                 @Nullable Consumer<WireIn> dataConsumer) {
        return Wires.readData(this, metaDataConsumer, dataConsumer);
    }

    default boolean readDocument(long position,
                                 @Nullable Consumer<WireIn> metaDataConsumer,
                                 @Nullable Consumer<WireIn> dataConsumer) {
        return Wires.readData(position, this, metaDataConsumer, dataConsumer);
    }
}
