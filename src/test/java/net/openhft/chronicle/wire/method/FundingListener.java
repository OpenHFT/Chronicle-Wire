/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.method;

/**
 * Interface defining a listener for funding-related events.
 * It provides methods for handling different types of funding notifications.
 */
public interface FundingListener {

    /**
     * Handles an event with detailed funding information.
     * This method is called when there is a funding event with a full Funding object.
     *
     * @param funding The Funding object containing detailed information about the funding event.
     */
    void funding(Funding funding);

    /**
     * Handles a funding event represented as a primitive integer.
     * This method can be used for simpler notifications where the funding information
     * is represented as a single integer value.
     *
     * @param num The integer representation of the funding information.
     */
    void fundingPrimitive(int num);

    /**
     * Handles a funding event with no additional arguments.
     * This method can be used for notifications where the event itself is sufficient
     * and no additional data is needed.
     */
    void fundingNoArg();
}
