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

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.annotation.UsedViaReflection;

public interface MessageHistory extends Marshallable {

    /**
     * Returns the {@code MessageHistory} to update it or read it.
     *
     * @return the MessageHistory for the current Excerpt.
     */
    static MessageHistory get() {
        return VanillaMessageHistory.getThreadLocal();
    }

    /**
     * You only need to call this if you wish to override it's behaviour.
     *
     * @param md to change to the default implementation for this thread. Null to clear the thread local
     *           and force withInitial to be called again
     */
    static void set(MessageHistory md) {
        VanillaMessageHistory.setThreadLocal(md);
    }

    @UsedViaReflection
    static void writeHistory(DocumentContext dc) {
        Wire wire = dc.wire();
        if (wire.bytes().readRemaining() == 0) // only add to the start of a message. i.e. for chained calls.
            wire.writeEventName(MethodReader.HISTORY)
                    .marshallable(get());
    }

    /**
     * Returns the number of timings contained in this {@code MessageHistory}.
     *
     * @return the number of timings contained in this {@code MessageHistory}.
     */
    int timings();

    /**
     * Returns a timing at a position specified by the input {@code n}.
     *
     * @return a timing at a position specified by the input {@code n}.
     */
    long timing(int n);

    /**
     * Returns the number of sources contained in this {@code MessageHistory}.
     *
     * @return the number of sources contained in this {@code MessageHistory}.
     */
    int sources();

    /**
     * Returns the source id at a position specified by the input {@code n}.
     *
     * @return the source id at a position specified by the input {@code n}.
     */
    int sourceId(int n);

    /**
     * Returns {@code true} if the source ids contained in this
     * {@code MessageHistory} end with the provided {@code sourceIds}.
     *
     * @return {@code true} if the source ids contained in this
     * {@code MessageHistory} end with the provided {@code sourceIds}.
     */
    boolean sourceIdsEndsWith(int[] sourceIds);

    /**
     * Returns the index of the source at a position specified by the
     * input {@code n}.
     *
     * @return the index of the source at a position specified by the
     * input {@code n}.
     */
    long sourceIndex(int n);

    /**
     * Clears all data contained in this {@code MessageHistory}
     */
    void reset();

    /**
     * Resets the {@code MessageHistory} with the provided {@code sourceId}
     * and {@code sourceIndex} as a starting point.
     */
    void reset(int sourceId, long sourceIndex);

    /**
     * Returns the last source id contained in this {@code MessageHistory}.
     *
     * @return the last source id contained in this {@code MessageHistory}.
     */
    int lastSourceId();

    /**
     * Returns the last source index contained in this {@code MessageHistory}.
     *
     * @return the last source index contained in this {@code MessageHistory}.
     */
    long lastSourceIndex();

}