/*
 * Copyright 2016 higherfrequencytrading.com
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

import net.openhft.chronicle.core.io.Closeable;

/**
 * Created by peter on 24/12/15.
 */
public interface DocumentContext extends Closeable {

    /**
     * @return true - is the entry is of type meta data
     */
    boolean isMetaData();

    /**
     * Set the metaData flag.  When writing this will determine whether the message is written as metaData or not.
     *
     * @param metaData write as metaData instead of data if true.
     */
    void metaData(boolean metaData);

    /**
     * @return true - if is a document document
     */
    boolean isPresent();

    /**
     * @return true - is the entry is of type data
     */
    default boolean isData() {
        return isPresent() && !isMetaData();
    }

    /**
     * @return the wire of the document
     */
    Wire wire();


}
