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

import net.openhft.chronicle.core.io.IORuntimeException;

/**
 * SourceContext exposes the numeric source ID and position for a data source.
 *
 * Implementations may wrap a queue {@code DocumentContext}, a network event or
 * another source of data.
 */
public interface SourceContext {

    /**
     * Unique identifier for the underlying source, or {@code -1} when none has
     * been established.
     *
     * @return the source identifier or {@code -1}
     */
    int sourceId();

    /**
     * Position in the source stream.
     * For queue readers this is the last index read; other sources may define a
     * different meaning.
     *
     * @return the position in implementation-defined units
     * @throws IORuntimeException if the position cannot be retrieved
     */
    long index() throws IORuntimeException;
}
