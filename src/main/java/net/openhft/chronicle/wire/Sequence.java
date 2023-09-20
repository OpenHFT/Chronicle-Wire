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
 * This is the Sequence interface.
 * <p>
 * It defines a contract for managing sequences in conjunction with write positions.
 * This interface is crucial when dealing with data structures where sequentiality and write positions matter,
 * such as in a message queue.
 * </p>
 * @since 2023-09-11
 */
public interface Sequence {

    // Constant representing that a sequence for a given write position is not found and the client can retry.
    long NOT_FOUND_RETRY = Long.MIN_VALUE;

    // Constant representing that a sequence for a given write position is not found and the client should not retry.
    long NOT_FOUND = -1;

    /**
     * Retrieves the sequence for a given write position.
     *
     * Note: This method is specifically designed to fetch the sequence for the last write position
     * in the queue. It is not suitable for obtaining sequences for arbitrary write positions.
     *
     * @param forWritePosition the last write position, typically representing the end of a queue.
     * @return NOT_FOUND_RETRY if the sequence for this write position cannot be found and retrying might succeed later;
     *         NOT_FOUND if the sequence is not present and retrying will not help.
     */
    long getSequence(long forWritePosition);

    /**
     * Assigns a sequence number to a particular write position.
     *
     * @param sequence the sequence number to be set.
     * @param position the write position with which the sequence number is associated.
     */
    void setSequence(long sequence, long position);

    /**
     * Translates a given header number and sequence to its index representation.
     *
     * @param headerNumber the header number.
     * @param sequence the sequence number.
     * @return The index corresponding to the provided header number and sequence.
     */
    long toIndex(long headerNumber, long sequence);

    /**
     * Converts an index to its corresponding sequence number.
     *
     * @param index the index to be converted.
     * @return The sequence number corresponding to the given index.
     */
    long toSequenceNumber(long index);
}
