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
 * EventPoller interface for handling polling events on a ChronicleChannel.
 * This interface extends the Closeable interface, hence any class implementing
 * EventPoller should provide implementation for the close() method from the Closeable interface.
 */
public interface EventPoller extends Closeable {

    /**
     * This method is called to poll an event on the given ChronicleChannel.
     *
     * @param channel The ChronicleChannel on which the event needs to be polled.
     * @return a boolean indicating the success/failure of the operation. True indicates
     * that the polling was successful in doing something, and false indicates it was not, i.e. there was nothing to do.
     */
    boolean onPoll(ChronicleChannel channel);
}
