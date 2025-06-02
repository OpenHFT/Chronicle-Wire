/*
 * Copyright 2016-2022 chronicle.software
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

package net.openhft.chronicle.wire.method;

/**
 * Defines the contract for an Event with sending and transaction time capabilities.
 * This interface establishes the methods required to set and retrieve the times associated with an event,
 * specifically in nanoseconds.
 */
public interface Event {

    /**
     * Sets the sending time for the event.
     * @param sendingTimeNS The sending time in nanoseconds.
     */
    void sendingTimeNS(long sendingTimeNS);

    /**
     * Retrieves the sending time of the event.
     * @return The sending time in nanoseconds.
     */
    long sendingTimeNS();

    /**
     * Sets the transaction time for the event.
     * @param transactTimeNS The transaction time in nanoseconds.
     */
    void transactTimeNS(long transactTimeNS);

    /**
     * Retrieves the transaction time of the event.
     * @return The transaction time in nanoseconds.
     */
    long transactTimeNS();
}
