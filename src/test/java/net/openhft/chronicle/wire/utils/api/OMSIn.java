/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
 */

package net.openhft.chronicle.wire.utils.api;

import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.wire.utils.dto.CancelOrderRequest;
import net.openhft.chronicle.wire.utils.dto.NewOrderSingle;

/**
 * Interface representing the input operations for an Order Management System (OMS).
 * It defines methods to handle new order requests and cancel order requests.
 */
public interface OMSIn {
    /**
     * Processes a new order single request.
     *
     * @param nos The NewOrderSingle object containing order details.
     */
    @MethodId(1)
    void newOrderSingle(NewOrderSingle nos);

    /**
     * Processes a cancel order request.
     *
     * @param cor The CancelOrderRequest object containing cancellation details.
     */
    @MethodId(2)
    void cancelOrderRequest(CancelOrderRequest cor);
}
