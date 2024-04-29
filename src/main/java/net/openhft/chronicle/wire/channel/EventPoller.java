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

package net.openhft.chronicle.wire.channel;

import net.openhft.chronicle.core.io.Closeable;

/**
 * The EventPoller interface defines a mechanism for polling events on a ChronicleChannel.
 * It extends the Closeable interface, therefore any class implementing EventPoller must
 * implement the close() method from the Closeable interface, which is used to release any system resources associated with the EventPoller.
 */
@SuppressWarnings("deprecation")
public interface EventPoller extends Closeable {

    /**
     * Polls for an event on the specified ChronicleChannel. This method should be invoked periodically
     * to ensure the successful processing of events in a timely manner. Implementations of this method
     * should handle all event processing logic.
     *
     * @param channel The ChronicleChannel instance to poll for events.
     * @return a boolean indicating the outcome of the polling operation. It returns 'true' if the poll operation
     * resulted in an event being processed, and 'false' if no event was available for processing, i.e. there was nothing to do.
     */
    boolean onPoll(ChronicleChannel channel);
}
