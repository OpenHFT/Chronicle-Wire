/*
 * Copyright 2016 higherfrequencytrading.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.ReadBytesMarshallable;
import net.openhft.chronicle.bytes.StopCharTesters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/*
 * Created by Peter Lawrey on 22/04/16.
 * <p>
 * Anything you can read marshallable object from.
 */
@FunctionalInterface
public interface MarshallableIn {
    @NotNull
    DocumentContext readingDocument();

    default boolean peekDocument() {
        return true;
    }

    /**
     * @param reader user to read the document
     * @return {@code true} if successful
     */
    default boolean readDocument(@NotNull ReadMarshallable reader) {
        try (@NotNull DocumentContext dc = readingDocument()) {
            if (!dc.isPresent())
                return false;
            reader.readMarshallable(dc.wire());
        }
        return true;
    }

    /**
     * @param reader used to read the document
     * @return {@code true} if successful
     */
    default boolean readBytes(@NotNull ReadBytesMarshallable reader) {
        try (@NotNull DocumentContext dc = readingDocument()) {
            if (!dc.isPresent())
                return false;
            reader.readMarshallable(dc.wire().bytes());
        }
        return true;
    }

    /**
     * @param using used to read the document
     * @return {@code true} if successful
     */
    default boolean readBytes(@NotNull Bytes using) {
        using.clear();
        try (@NotNull DocumentContext dc = readingDocument()) {
            if (!dc.isPresent())
                return false;
            Bytes<?> bytes = dc.wire().bytes();
            long len = Math.min(using.writeRemaining(), bytes.readRemaining());
            using.write(bytes, bytes.readPosition(), len);
            bytes.readSkip(len);
        }
        return true;
    }

    /**
     * Read the next message as a String
     *
     * @return the String or null if there is none.
     */
    @Nullable
    default String readText() {
        try (@NotNull DocumentContext dc = readingDocument()) {
            if (!dc.isPresent()) {
                return null;
            }
            StringBuilder sb = Wires.acquireStringBuilder();
            dc.wire().bytes().parse8bit(sb, StopCharTesters.ALL);
            while (sb.length() > 0 && sb.charAt(sb.length() - 1) == 0)
                sb.setLength(sb.length() - 1);
            return WireInternal.INTERNER.intern(sb);
        }
    }

    /**
     * Read the next message as  string
     *
     * @param sb to copy the text into
     * @return true if there was a message, or false if not.
     */
    default boolean readText(@NotNull StringBuilder sb) {
        try (@NotNull DocumentContext dc = readingDocument()) {
            if (!dc.isPresent()) {
                sb.setLength(0);
                return false;
            }
            dc.wire().bytes().parse8bit(sb, StopCharTesters.ALL);
        }
        return true;
    }

    /**
     * Read a Map&gt;String, Object&gt; from the content.
     *
     * @return the Map, or null if no message is waiting.
     */
    @Nullable
    default <K, V> Map<K, V> readMap() {
        try (@NotNull DocumentContext dc = readingDocument()) {
            if (!dc.isPresent()) {
                return null;
            }
            final Wire wire = dc.wire();
            if (!wire.hasMore())
                return Collections.emptyMap();
            @NotNull Map<K, V> ret = new LinkedHashMap<>();
            while (wire.hasMore()) {
                @NotNull K key = (K) wire.readEvent(Object.class);
                @Nullable V value = (V) wire.getValueIn().object();
                ret.put(key, value);
            }
            return ret;
        }
    }

    /**
     * Reads messages from this tails as methods.  It returns a BooleanSupplier which returns
     *
     * @param objects which implement the methods serialized to the file.
     * @return a reader which will read one Excerpt at a time
     */
    @NotNull
    default MethodReader methodReader(Object... objects) {
        return new VanillaMethodReader(this, false, VanillaMethodReaderBuilder.createDefaultParselet(false), null, objects);
    }

    @NotNull
    default VanillaMethodReaderBuilder methodReaderBuilder() {
        return new VanillaMethodReaderBuilder(this);
    }
}

