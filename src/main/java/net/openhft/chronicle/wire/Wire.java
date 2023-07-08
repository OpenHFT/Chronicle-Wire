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
import net.openhft.chronicle.core.annotation.DontChain;
import net.openhft.chronicle.core.annotation.SingleThreaded;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Defines a standard interface for sequential reading and writing to/from a Bytes stream.
 * A Wire object encapsulates the operations for manipulating a stream of bytes, supporting different formats such as JSON, Binary YAML, and YAML.
 * Implementations of this interface are expected to be used in a single-threaded context and not chained.
 * <p>
 */
@SingleThreaded
@DontChain
public interface Wire extends WireIn, WireOut {

    /**
     * Creates a Wire object from a file, the type of the Wire is inferred from the file extension.
     * This method is deprecated and might be removed in the future.
     *
     * @param name The name of the file
     * @return A Wire object for the given file
     * @throws IOException If an I/O error occurs
     * @throws IllegalArgumentException If the file type is unknown
     */
    @Deprecated(/*to be removed?*/)
    static Wire fromFile(@NotNull String name) throws IOException {
        @NotNull String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        switch (ext) {
            case "csv":
                return CSVWire.fromFile(name);
            case "yaml":
                return TextWire.fromFile(name);
            default:
                throw new IllegalArgumentException("Unknown file type " + name);
        }
    }

    /**
     * Creates a new YamlWire that writes to an on-heap Bytes object.
     *
     * @return A new instance of YamlWire
     */
    static Wire newYamlWireOnHeap() {
        return new YamlWire();
    }

    /**
     * Sets the header number for the Wire instance.
     *
     * @param headerNumber The header number to be set
     * @return The current Wire instance with updated header number
     */
    @Override
    @NotNull
    Wire headerNumber(long headerNumber);
}

