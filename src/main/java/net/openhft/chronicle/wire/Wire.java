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
 * The defines the stand interface for writing and reading sequentially to/from a Bytes stream. It is used to write and read data in a wire format.
 */
@SingleThreaded
@DontChain
public interface Wire extends WireIn, WireOut {
    /**
     * Create a YamlWire that write to an on heap Bytes
     *
     * @return the Wire
     */
    static Wire newYamlWireOnHeap() {
        return new YamlWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
    }

    @Override
    @NotNull
    Wire headerNumber(long headerNumber);
}
