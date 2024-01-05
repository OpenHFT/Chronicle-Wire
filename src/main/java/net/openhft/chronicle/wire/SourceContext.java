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

import net.openhft.chronicle.core.io.IORuntimeException;

/**
 * This is the SourceContext interface.
 * It defines methods to interact with the underlying source context, particularly in terms of accessing its source ID
 * and the last read index. Implementations of this interface are expected to handle source contexts which could be used
 * in various scenarios like I/O operations, data streaming, or context management.
 */
public interface SourceContext {

    /**
     * Retrieves the source ID associated with this context.
     * A unique identifier that represents the source from which data or operations might be fetched or to which data
     * might be written. If a valid source ID has not been established or isn't available, it defaults to returning -1.
     *
     * @return The unique identifier for this source context, or -1 if not available.
     */
    int sourceId();

    /**
     * Obtains the index of the last read operation from this source context.
     * This is particularly useful to track the reading progress and can act as a checkpoint or reference point.
     * Note: This method is specifically intended for read contexts and might not be relevant for other context types.
     *
     * @return The index corresponding to the last read operation.
     * @throws IORuntimeException if any issue arises while fetching the index.
     */
    long index() throws IORuntimeException;
}
