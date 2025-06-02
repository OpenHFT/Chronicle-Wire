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

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.annotation.UsedViaReflection;

/**
 * Records the path a message takes as it moves through components.
 * Each processing step may append its source identifier and timestamp,
 * allowing tracing and latency analysis in distributed systems.
 */
public interface MessageHistory extends Marshallable {

    /**
     * Retrieves the thread-local {@code MessageHistory} instance for the
     * message being processed.
     *
     * @return the history associated with the current excerpt.
     */
    static MessageHistory get() {
        return VanillaMessageHistory.getThreadLocal();
    }

    /**
     * You only need to call this if you wish to override its behaviour.
     *
     * @param md the history instance to use for this thread, or {@code null}
     *           to clear the thread local so {@link #get()} will create a new
     *           default instance
     */
    static void set(MessageHistory md) {
        VanillaMessageHistory.setThreadLocal(md);
    }

    /**
     * Effectively calls {@code set(null)} removing the current thread-local
     * history.
     */
    static void clear() {
        VanillaMessageHistory.setThreadLocal(null);
    }

    /**
     * Sets the current thread-local history to a new empty
     * {@link VanillaMessageHistory}.
     */
    static void emptyHistory() {
        VanillaMessageHistory.setThreadLocal(new VanillaMessageHistory());
    }

    /**
     * Writes the current thread's history to the given document if the
     * document is empty.
     *
     * @param dc the {@link DocumentContext} to write the history to
     */
    @UsedViaReflection
    static void writeHistory(DocumentContext dc) {
        if (((WriteDocumentContext) dc).isEmpty()) { // only add to the start of a message. i.e. for chained calls.
            get().doWriteHistory(dc);
        }
    }

    // needed by NoMessageHistory in Queue
    /**
     * Default implementation for recording history into the supplied
     * {@link DocumentContext}. It writes an event named
     * {@link net.openhft.chronicle.bytes.MethodReader#HISTORY} with this history
     * as the value.
     *
     * @param dc the document to append the history to
     */
    default void doWriteHistory(DocumentContext dc) {
        dc.wire().writeEventName(MethodReader.HISTORY).marshallable(get());
    }

    /**
     * Returns the count of timing entries recorded in this history.
     *
     * @return the number of timings
     */
    int timings();

    /**
     * @param n zero-based index of the timing entry to read
     * @return the timestamp recorded at index {@code n}
     */
    long timing(int n);

    /**
     * Returns the count of source entries recorded in this history.
     *
     * @return the number of sources
     */
    int sources();

    /**
     * @param n zero-based index of the source entry
     * @return the identifier of the source recorded at index {@code n}
     */
    int sourceId(int n);

    /**
     * @param sourceIds sequence of IDs to check against the end of this history
     * @return {@code true} if the IDs match the end of the stored list
     */
    boolean sourceIdsEndsWith(int[] sourceIds);

    /**
     * @param n zero-based index of the source entry
     * @return the index within the source component recorded at index {@code n}
     */
    long sourceIndex(int n);

    /**
     * Clears all recorded source and timing entries.
     */
    @Override
    void reset();

    /**
     * Clears existing entries and initialises this history with a single source
     * and the current time.
     *
     * @param sourceId    identifier of the initial source component
     * @param sourceIndex index within that source
     */
    void reset(int sourceId, long sourceIndex);

    /**
     * Returns the ID of the most recently added source or {@code -1} if none.
     */
    int lastSourceId();

    /**
     * Returns the index of the most recently added source or {@code -1} if none.
     */
    long lastSourceIndex();

    /**
     * Indicates if new entries were added since the last write or reset.
     *
     * @return {@code true} if the history has unwritten changes
     */
    boolean isDirty();
}
