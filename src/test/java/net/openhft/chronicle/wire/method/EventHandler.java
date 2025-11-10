//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
