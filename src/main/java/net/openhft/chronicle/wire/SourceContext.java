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

import net.openhft.chronicle.core.io.IORuntimeException;

/**
 * Interface for accessing metadata about the origin of a message.
 * <p>
 * It exposes the source identifier and position of the current document.
 * A {@link DocumentContext} may implement this interface to provide
 * traceability of a message to its queue index and source ID.
 */
public interface SourceContext {

    /**
     * Retrieves the source ID associated with this context.
     * A unique identifier that represents the source from which data or operations might be fetched or to which data
     * might be written. If a valid source ID has not been established or isn't available, it defaults to returning -1.
     *
     * @return unique identifier for this context, or {@code -1} if not available or not applicable
     */
    int sourceId();

    /**
     * Obtains the index of the last read operation from this source context.
     * This is particularly useful to track the reading progress and can act as a checkpoint or reference point.
     * Note: This method is specifically intended for read contexts and might not be relevant for other context types.
     *
     * @return index of the current entry, for example the queue index
     * @throws IORuntimeException if the index cannot be determined
     */
    long index() throws IORuntimeException;
}
