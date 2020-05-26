/*
 * Copyright 2016-2020 Chronicle Software
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

public interface Sequence {
    long NOT_FOUND_RETRY = Long.MIN_VALUE;
    long NOT_FOUND = -1;

    /**
     * gets the sequence for a writePosition
     * <p>
     * This method will only return a valid sequence number of the write position if the write position is the
     * last write position in the queue. YOU CAN NOT USE THIS METHOD TO LOOK UP RANDOM SEQUENCES FOR ANY WRITE POSITION.
     * NOT_FOUND_RETRY will be return if a sequence number can not be found  ( so retry )
     * or NOT_FOUND if you should not retry
     *
     * @param forWritePosition the last write position, expected to be the end of queue
     * @return NOT_FOUND_RETRY if the sequence for this write position can not be found (you can retry), or
     * NOT_FOUND if it can't be found and there is no point in retrying
     */
    long getSequence(long forWritePosition);

    /**
     * sets the sequence number for a writePosition
     *
     * @param sequence the sequence number
     * @param position the write position
     */
    void setSequence(long sequence, long position);

    long toIndex(long headerNumber, long sequence);

    long toSequenceNumber(long index);
}
