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
 * Top level interface for bidirectional wire implementations.  It combines the
 * functionality of {@link WireIn} and {@link WireOut} and represents the most
 * common entry point for using Chronicle Wire.  All operations are expected to
 * be single threaded on a given instance.
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
     * Sets the sequence number used when writing headers.  Queue and TCP
     * protocols typically increment this value for each new document.
     */
    @Override
    @NotNull
    Wire headerNumber(long headerNumber);
}
