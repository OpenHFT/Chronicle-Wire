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

import net.openhft.chronicle.wire.Base64LongConverter;
import net.openhft.chronicle.wire.BytesInBinaryMarshallable;
import net.openhft.chronicle.wire.LongConversion;
import net.openhft.chronicle.wire.NanoTimestampLongConverter;

/**
 * Represents an event within the Chronicle system, providing details
 * like sending and transaction times.
 * Extends `BytesInBinaryMarshallable` to support binary serialization
 * and implements the `Event` interface for event-specific behaviors.
 */
public class ChronicleEvent extends BytesInBinaryMarshallable implements Event {

    // Timestamp indicating when the event was sent, converted using NanoTimestampLongConverter
    @LongConversion(NanoTimestampLongConverter.class)
    private long sendingTimeNS;

    // Timestamp indicating when the event was processed or transacted, converted using NanoTimestampLongConverter
    @LongConversion(NanoTimestampLongConverter.class)
    private long transactTimeNS;

    // Long field encoded with Base64, likely representing some text data or key
    @LongConversion(Base64LongConverter.class)
    private long text1;

    // Generic string field
    private String text3;

    /**
     * Sets the sending time for the event.
     * @param sendingTimeNS The time the event was sent, in nanoseconds.
     */
    @Override
    public void sendingTimeNS(long sendingTimeNS) {
        this.sendingTimeNS = sendingTimeNS;
    }

    /**
     * Retrieves the sending time for the event.
     * @return The time the event was sent, in nanoseconds.
     */
    @Override
    public long sendingTimeNS() {
        return sendingTimeNS;
    }

    /**
     * Sets the transaction time for the event.
     * @param transactTimeNS The time the event was processed or transacted, in nanoseconds.
     */
    @Override
    public void transactTimeNS(long transactTimeNS) {
        this.transactTimeNS = transactTimeNS;
    }

    /**
     * Retrieves the transaction time for the event.
     * @return The time the event was processed or transacted, in nanoseconds.
     */
    @Override
    public long transactTimeNS() {
        return transactTimeNS;
    }
}
