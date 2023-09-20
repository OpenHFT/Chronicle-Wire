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

/**
 * An enumeration implementation of the {@link DocumentContext} interface.
 * This context represents a non-existent or uninitialized document context,
 * hence all its methods return default or null values.
 *
 * <p>This can be useful as a sentinel value or placeholder to avoid null checks in code
 * that works with document contexts. Using `NoDocumentContext.INSTANCE` denotes
 * a guaranteed uninitialized state for a document context.
 *
 * @since 2023-09-11
 */
public enum NoDocumentContext implements DocumentContext {
    /** The singleton instance of the NoDocumentContext */
    INSTANCE;

    @Override
    public boolean isMetaData() {
        return false;
    }

    @Override
    public boolean isPresent() {
        return false;
    }

    @Override
    public boolean isData() {
        return false;
    }

    @Override
    public Wire wire() {
        return null;
    }

    @Override
    public int sourceId() {
        return -1;
    }

    @Override
    public long index() {
        return Long.MIN_VALUE;
    }

    @Override
    public boolean isNotComplete() {
        return false;
    }

    @Override
    public void close() {
        // Do nothing
    }

    @Override
    public void reset() {
        close();
    }

}
