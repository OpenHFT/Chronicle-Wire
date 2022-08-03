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

public interface SourceContext {

    /**
     * Returns the source id of this source context. If the id does not
     * exist, -1 is returned instead.
     *
     * @return the current source id of this source context or -1 if the
     * source id does not exist.
     */
    int sourceId();

    /**
     * Index last read, only available for read contexts.
     *
     * @return the current Excerpt's index
     * @throws IORuntimeException if an error occurred while getting the index
     */
    long index() throws IORuntimeException;
}
