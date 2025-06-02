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
package net.openhft.chronicle.wire;

/**
 * Defines a contract for mapping a write position (typically in a persistent
 * store such as Chronicle Queue) to a logical sequence number, and vice versa.
 * This is crucial for systems that need to assign ordered sequence numbers to
 * messages or events written at potentially non-contiguous positions.
 */
public interface Sequence {
    /**
     * Returned by {@link #getSequence(long)} when a sequence number cannot be
     * found for the given position but the operation may succeed if retried.
     */
    long NOT_FOUND_RETRY = Long.MIN_VALUE;

    /**
     * Returned by {@link #getSequence(long)} when a sequence number cannot be
     * found for the given position and retrying is unlikely to succeed.
     */
    long NOT_FOUND = -1;

    /**
     * Gets the sequence number for the supplied write position. This method is
     * typically used with the last write position and may not be suitable for
     * arbitrary positions.
     *
     * @param forWritePosition The write position (for example an excerpt's
     *                         starting offset) for which the sequence number is
     *                         requested. This is usually the most recently
     *                         written position.
     * @return The sequence number for {@code forWritePosition}, or
     *         {@link #NOT_FOUND_RETRY} if the lookup should be retried, or
     *         {@link #NOT_FOUND} if the position does not map to a sequence
     *         number.
     */
    long getSequence(long forWritePosition);

    /**
     * Sets the sequence number for the given write position.
     *
     * @param sequence The sequence number to associate with the position.
     * @param position The write position for which the sequence number should be
     *                 recorded.
     */
    void setSequence(long sequence, long position);

    /**
     * Combines a {@code headerNumber} (such as a cycle count) and a
     * {@code sequence} within that header into a single index value.
     *
     * @param headerNumber The higher-order part of the index, for example a
     *                     cycle number.
     * @param sequence The lower-order sequence number within the header.
     * @return A combined index representing the header and sequence.
     */
    long toIndex(long headerNumber, long sequence);

    /**
     * Extracts the lower-order sequence number from an index previously created
     * by {@link #toIndex(long, long)}.
     *
     * @param index The combined index.
     * @return The sequence number extracted from the index.
     */
    long toSequenceNumber(long index);
}
