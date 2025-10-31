/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
 * <p>This interface combines the capabilities of both {@link WireIn} and {@link WireOut} interfaces.
 */
@SingleThreaded
@DontChain
public interface Wire extends WireIn, WireOut {
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

    /**
     * Indicates whether tuples should be generated.
     * By default, this method returns the value of Wires.GENERATE_TUPLES
     * @return true if tuples should be generated, false otherwise.
     */
    default boolean generateTuples() {
        return Wires.GENERATE_TUPLES;
    }

    /**
     * Sets whether tuples should be generated.
     * @param generateTuples true to enable tuple generation, false to disable it.
     */
    default Wire generateTuples(boolean generateTuples) {
        return this;
    }
}
