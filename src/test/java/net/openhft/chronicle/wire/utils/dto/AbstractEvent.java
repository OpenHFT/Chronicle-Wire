/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
 */

package net.openhft.chronicle.wire.utils.dto;

import net.openhft.chronicle.wire.*;
import net.openhft.chronicle.wire.converter.NanoTime;
import net.openhft.chronicle.wire.converter.ShortText;

/**
 * Abstract base class for events, providing common fields and methods for event-related data.
 * This class is designed to be extended by specific event types.
 *
 * @param <E> The type of the event extending this class, used for fluent interface pattern.
 */
@SuppressWarnings("unchecked")
public class AbstractEvent<E extends AbstractEvent<E>> extends SelfDescribingMarshallable {
    @ShortText
    private long sender;
    @ShortText
    private long target;
    // Client sending time
    @NanoTime
    private long sendingTime;

    /**
     * Retrieves the sender identifier.
     *
     * @return The sender's unique identifier.
     */
    public long sender() {
        return sender;
    }

    /**
     * Sets the sender identifier and returns the updated event.
     *
     * @param sender The sender's unique identifier.
     * @return The current instance with updated sender.
     */
    public E sender(long sender) {
        this.sender = sender;
        return (E) this;
    }

    /**
     * Retrieves the target identifier.
     *
     * @return The target's unique identifier.
     */
    public long target() {
        return target;
    }

    /**
     * Sets the target identifier and returns the updated event.
     *
     * @param target The target's unique identifier.
     * @return The current instance with updated target.
     */
    public E target(long target) {
        this.target = target;
        return (E) this;
    }

    /**
     * Retrieves the sending time of the event.
     *
     * @return The time when the event was sent.
     */
    public long sendingTime() {
        return sendingTime;
    }

    /**
     * Sets the sending time of the event and returns the updated event.
     *
     * @param sendingTime The time when the event was sent.
     * @return The current instance with updated sending time.
     */
    public E sendingTime(long sendingTime) {
        this.sendingTime = sendingTime;
        return (E) this;
    }
}
