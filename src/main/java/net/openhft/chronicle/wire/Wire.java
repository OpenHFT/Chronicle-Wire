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
 * Defines the standard interface for sequentially writing to and reading from a Bytes stream.
 * Implementations of this interface should ensure single-threaded access and avoid method chaining.
 *
 * <p>This interface combines the capabilities of both {@link WireIn} and {@link WireOut} interfaces.</p>
 */
@SingleThreaded
@DontChain
public interface Wire extends WireIn, WireOut {

    /**
     * Factory method to create a Wire instance based on the file extension provided in the file name.
     * Currently, supports "csv" and "yaml" extensions.
     *
     * @param name The name of the file, including its extension.
     * @return A Wire implementation corresponding to the file extension.
     * @throws IOException If there's an error accessing the file.
     * @throws IllegalArgumentException If the file type is unknown.
     * @deprecated This method might be removed in future releases. Consider other ways to create a Wire instance.
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
     * Factory method to create a new YamlWire instance that writes to an on-heap Bytes object.
     *
     * @return A YamlWire instance configured to write to an on-heap Bytes object.
     */
    static Wire newYamlWireOnHeap() {
        return new YamlWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
    }

    /**
     * Set the header number for the Wire. Concrete implementations should ensure they provide the expected behavior for this method.
     *
     * @param headerNumber The header number to be set.
     * @return The current Wire instance, often for method chaining.
     */
    @Override
    @NotNull
    Wire headerNumber(long headerNumber);
}
