/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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

import net.openhft.chronicle.core.Mocker;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;

public interface DocumentContext extends Closeable, SourceContext {
    DocumentContext NOOP = Mocker.ignored(DocumentContext.class);

    /**
     * Checks it the {@code DocumentContext} is metadata. If it is, {@code true} is
     * returned, otherwise {@code false}.
     *
     * @return true if the entry is metadata
     */
    boolean isMetaData();

    /**
     * Checks if the {@code DocumentContext} is present. If it is, {@code true} is returned,
     * otherwise {@code false}.
     *
     * @return true if the entry is present
     */
    boolean isPresent();

    /**
     * Checks if the {@code DocumentContext} is data. If it is, {@code true} is returned,
     * otherwise {@code false}
     *
     * @return true - is the entry is data
     */
    default boolean isData() {
        return isPresent() && !isMetaData();
    }

    /**
     * Returns the {@link Wire} associated with the {@code Document}. It is possible that
     * {@code null} is returned, depending on the implementation.
     *
     * @return the {@link Wire} associated with the {@code Document}.
     */
    @Nullable
    Wire wire();

    /**
     * @return whether the NOT_COMPLETE flag has been set.
     */
    boolean isNotComplete();

    default boolean isOpen() {
        return isNotComplete();
    }

    /**
     * Call this if you have detected an error condition and you want the context
     * rolled back when it is closed, rather than half a message committed
     */
    default void rollbackOnClose() {
    }

    @Override
    void close();
}
