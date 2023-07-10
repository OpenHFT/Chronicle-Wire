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
 * The SourceContext interface provides a way to interact with the context
 * of a data source. Implementations of this interface should provide methods
 * to access and manipulate the source's context details.
 */
public interface SourceContext {

    /**
     * Retrieves the unique identifier associated with this source context.
     * If the source context does not have an identifier, this method returns -1.
     *
     * @return The unique identifier of the source context if it exists,
     *         otherwise -1.
     */
    int sourceId();

    /**
     * Fetches the index of the last item that was read from this context.
     * This method is only applicable for read contexts.
     *
     * @return The index of the most recently read Excerpt from the context.
     * @throws IORuntimeException if there's an issue retrieving the index,
     *         typically due to input/output errors.
     */
    long index() throws IORuntimeException;
}
