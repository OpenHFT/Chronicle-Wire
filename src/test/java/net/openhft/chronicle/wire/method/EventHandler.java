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
 * Interface for handling events of type T where T is a subtype of Event.
 * Implementors of this interface should provide concrete behavior for what to do with the event.
 *
 * @param <T> The type of event this handler is responsible for, bounded by the Event interface.
 */
public interface EventHandler<T extends Event> {

    /**
     * Handles the event. This method is meant to define how an event should be processed.
     *
     * @param event The event to handle.
     */
    void event(T event);

    /**
     * Responds to an event. This method is typically invoked when an event occurs.
     *
     * @param event The event that has occurred.
     */
    void onEvent(T event);
}
