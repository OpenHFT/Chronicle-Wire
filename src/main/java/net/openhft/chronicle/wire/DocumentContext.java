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

import net.openhft.chronicle.core.Mocker;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
/**
 * DocumentContext is an interface that represents a specific context within a document.
 * It provides the ability to inspect and interact with the state of the document. This
 * includes checking whether the document context is metadata, data or present, retrieving
 * the associated Wire, and other related operations.
 */
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
     * @return {@code true} if the DocumentContext represents data,
     *         otherwise {@code false}.
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
     * Checks if the NOT_COMPLETE flag has been set for the DocumentContext.
     *
     * @return {@code true} if the NOT_COMPLETE flag is set,
     *         otherwise {@code false}.
     */
    boolean isNotComplete();

    /**
     * Verifies if the DocumentContext is currently open, by checking the NOT_COMPLETE flag.
     *
     * @return {@code true} if the DocumentContext is open,
     *         otherwise {@code false}.
     */
    default boolean isOpen() {
        return isNotComplete();
    }

    /**
     * Invokes this method when an error condition is detected and
     * you want the context to be rolled back when it is closed,
     * rather than committing half a message.
     */
    default void rollbackOnClose() {
    }

    /**
     * Closes the DocumentContext. Specifics of the closing operation
     * depend on the implementation.
     */
    @Override
    void close();

    /**
     * Resets the DocumentContext. This involves closing the context
     * once and discarding any remaining state.
     */
    void reset();
}
