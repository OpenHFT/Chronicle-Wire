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
import net.openhft.chronicle.core.annotation.SingleThreaded;
import org.jetbrains.annotations.NotNull;


/**
 * Primary interface for sequential writing to and reading from a {@link Bytes} stream.
 *
 * <p>The interface merges {@link WireIn} and {@link WireOut} into a bidirectional wire and
 * forms the usual top-level entry point for general wire operations. Implementations are
 * single-threaded and should avoid method chaining.
 */
@SingleThreaded
@DontChain
public interface Wire extends WireIn, WireOut {
    /**
     * Creates a YAML wire backed by on-heap bytes.
     * <p>
     * The returned wire reads and writes text documents.
     *
     * @return a YamlWire configured for on-heap bytes
     */
    static Wire newYamlWireOnHeap() {
        return new YamlWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
    }

    /**
     * Assigns the sequence number used when writing the next header.
     * Overrides {@link WireCommon#headerNumber(long)} with a fluent return type.
     *
     * @param headerNumber sequence number to set
     * @return this wire instance
     */
    @Override
    @NotNull
    Wire headerNumber(long headerNumber);
}
