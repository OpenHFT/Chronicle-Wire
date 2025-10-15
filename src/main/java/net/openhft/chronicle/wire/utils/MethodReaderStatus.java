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

package net.openhft.chronicle.wire.utils;

/**
 * Enumerates the possible outcomes of a single read attempt by a
 * {@link net.openhft.chronicle.bytes.MethodReader}, particularly for
 * generated readers such as {@link net.openhft.chronicle.wire.AbstractGeneratedMethodReader}.
 */
public enum MethodReaderStatus {
    /**
     * Indicates that no message or event was found or processed in the current
     * read attempt (for example end of document or no data available).
     */
    EMPTY,
    /**
     * Indicates that a {@link net.openhft.chronicle.wire.MessageHistory} event
     * was read and processed.
     */
    HISTORY,
    /**
     * Indicates that a known method or event was successfully read,
     * deserialized and dispatched to a handler.
     */
    KNOWN,
    /**
     * Indicates that an unknown method or event was encountered. The default
     * parselet may have skipped it or an error might have been logged.
     */
    UNKNOWN
}
