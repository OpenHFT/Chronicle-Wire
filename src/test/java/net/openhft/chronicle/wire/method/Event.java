/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
